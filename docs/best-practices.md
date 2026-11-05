# 嵌入式流程内核最佳实践

本文面向在 Java 应用内嵌入 reform-engine-core 的开发者。内核负责内存执行树和流程推进，业务节点由宿主实现；没有内置审批、HTTP、定时器、持久化或进程重启恢复。案例为虚构文本处理，不代表真实业务验收。“覆盖”指有明确断言的场景，不表示每个接口重载和所有组合均已验证。

## 从可运行案例开始

配置 JDK 11、Maven 和 `JAVA_HOME` 后，在仓库根目录执行：

```sh
bash acceptance/run.sh
```

脚本先完整构建并安装引擎，再独立构建消费者、运行测试，并从正式 JAR 启动两个 main。具体环境参数和依赖见 [验收说明](../acceptance/README.md)。消费者未加入根 Maven reactor；仅执行根 `mvn test` 不会运行它。该检查不发布任何制品。

- [TextBatchApplication](../acceptance/consumer/src/main/java/acceptance/TextBatchApplication.java)：有条件路由、三个分支、嵌套 scope、调用式子流程和结果写入的完整组合案例。节点等待信号后才推进；对应测试验证全部六种分支完成顺序。
- [ApiUsageExamples](../acceptance/consumer/src/main/java/acceptance/ApiUsageExamples.java)：四个小案例，可从 main 查看结果；对应测试还包括取消和错误信号。便于复制一种用法，不必先理解整个组合图。
- [TextBatchAcceptanceTest](../acceptance/consumer/src/test/java/acceptance/TextBatchAcceptanceTest.java) 和 [ApiUsageExamplesTest](../acceptance/consumer/src/test/java/acceptance/ApiUsageExamplesTest.java)：可执行断言，合计 14 项。异常路径同样属于验收结果。

两个 main 均放在消费者 main 源码中，能够在没有 JUnit、引擎测试类或源码目录的 classpath 上运行。它们使用的 Parallel、等待任务、路由和子流程行为都是案例自定义代码。

## 构建定义并驱动实例

使用 `ProcessDefinitionBuilder` 为节点分配明确且唯一的 ID，设置一个 initial，并为可执行节点提供 `ActivityBehavior`。`createActivity/endActivity` 表示包含关系；`scope()` 表示执行作用域，不是额外启动一个独立流程。连线目标在 `buildProcessDefinition()` 时解析，不存在的目标会报错；这不是完整的流程图校验器。发布定义前由宿主校验入口、节点行为注册、ID、连线和业务约束。

定义级 `setKey/setName/setDescription` 用于描述模型；`property` 写入当前定义、活动或连线。配置连线属性和 take 监听器时，用 `startTransition(...)/endTransition()`，以免把属性挂到错误对象。`ApiUsageExamples.auditedSignal` 演示这两种配置位置。

同一定义可创建多个实例，但构建后不要再修改共享定义或行为注册表。行为对象也可能被多个实例共享，实例数据应放在执行变量中，不应写进共享行为字段。案例的 children 映射仅用于定位演示子实例，不是线程安全任务服务；真实宿主需自行管理实例引用、唯一业务键和引用释放。

典型驱动顺序为 `createProcessInstance → setVariable → start → findExecution(activityId).signal`。`findActiveActivityIds` 用于查看活动位置；`findExecution` 可能返回 null，且同一个 activityId 有多个执行时不能代替唯一任务定位。每次驱动前重新定位，避免长期保留被剪枝或替换的子执行引用。不要把根 `signal` 当作自动向任意等待节点广播的 API。

同一实例及其子执行树必须由宿主串行驱动。并行分支是逻辑上的并发路径，不意味着引擎自动创建线程。两个实例的绑定容器独立，但传入同一个可变对象仍共享引用；案例为子流程输入显式复制列表。

## 自定义节点、条件和等待

同步工作节点在 `execute` 中计算并写变量，然后明确 `take` 或 `end`。返回而不推进代表等待，不需要 sleep。等待节点实现 `SignallableActivityBehavior`，在 `signal` 中验证名称、载荷和业务条件，再产生副作用；`auditedSignal` 对错误名称或错误载荷先抛异常，保持未写入状态。

引擎不提供按名称去重、消息投递、认证或幂等键。已结束执行收到 signal 会被拒绝；仍等待的活动可以再次收到同名信号，是否接受由行为决定。不要对失败调用盲目重试，先判断发生了哪些副作用。

条件路由由业务行为读取变量、选择明确的 outgoing transition，再调用 `take`。案例选择 reject、quick 或 full；给连线设置属性不会自动产生表达式求值。并行示例先 `inactivate()`，用 `findInactiveConcurrentExecutions(activity)` 收集真实 joined 列表，达到其入边数量后调用 `takeAll(outgoing, joined)`。这是本图的同步策略，不能直接当成支持可选分支的 inclusive join：如果部分入边不会到达，就不能机械等待全部入边。

## 变量和结果

公共入口 `PvmExecution` 提供基本值读写；行为内的 `ActivityExecution`/`VariableScope` 提供本地、批量、类型化读写和删除。

- `getVariable` 沿 parent 链读取最近绑定；`setVariable` 更新最近已有绑定，否则写父链根。Local 版本只操作当前执行。
- `createVariableLocal` 严格拒绝本地同名键，包括已有 null；父级同名允许遮蔽。`setVariableLocal` 可覆盖，返回值不表示旧值；用 getter 读取。
- `hasVariableLocal` 可区分不存在与存在 null；删除本地遮蔽会重新露出父级绑定。批量 `setVariablesLocal` 是合并赋值，不是严格创建。
- 幸存分支剪枝时，局部变量可能迁移到父执行并覆盖父层同名值；不同分支结束顺序可能留下不同结果。不要用分支局部迁移实现聚合。各分支显式写入独立根变量名，再在汇合后计算结果。
- 结束后仍可读取根变量；变量值不深拷贝，异常不自动撤销写入。不要把 root 的结果值或 ended 单独视为业务成功。

`localVariables` 展示 null 遮蔽、删除后读取父值、更新父层绑定和清理临时值。全部变量重载、集合快照、严格创建、删除及迁移兼容规则见 [变量契约](variable-contract.md)，这里不重复列举。

## scope、复合行为与调用式子流程

scope 是当前流程执行树内的边界。嵌套 scope 的进入、跨界离开和路径收缩由内核处理；普通业务代码不要手动拼装执行树或修改 active/ended 标志。

`CompositeActivityBehavior` 可在本作用域内 `executeActivity`，在 `lastExecutionEnded` 中继续外层；`composite` 小案例验证单内部活动结束后走外部连线。它与调用式子流程不同：调用式行为通过 `createSubProcessInstance` 建立独立流程根，显式写入输入再 start，正常完成时通过 `completing` 复制输出、`completed` 推进调用节点。子流程不自动继承或合并调用者变量。见 `TextBatchApplication.validationCall`。

公开 `destroyScope` 用于中断并清理后代，保留调用执行供自定义行为继续；它不是宿主直接取消任意节点的快捷方法。本次独立案例未新增内部中断图，其正确使用和外层继续由 `CancellationProcessEndOwnerTest`、`TerminalExecutionContractTest` 等引擎回归保护。

## 监听、取消和异常

流程和活动支持 start/end，连线支持 take。监听器同步执行，可读取事件来源和名称；案例通过公开 `ExecutionListenerExecution` 查看来源 ID，区分 wait:end、accepted:take 和流程 end。监听器可抛异常，日志、计数或外部通知也可能已发生，不能假设 exactly-once。

流程定义 end 属于该流程根，取消成功也会通知；调用式子流程取消会通知它自己的 end，但不调用正常 completing/completed。scope end 是路径相关通知，不能全局按 scope ID 去重。正常完成、内部取消和整个流程取消的区别见 [生命周期事件契约](lifecycle-event-contract.md)。

| 可观察状态 | 使用方式 |
| --- | --- |
| inactive | 可能在 join 等待、承载活跃子树或已结束；不能单独判断没有存活路径 |
| 自然结束 | 根 ended=true、active=false；后续 signal 被拒绝 |
| 整流程取消成功 | 根同样进入终态；end 监听器不代表业务完成 |
| 内部 scope 清理成功 | 保留的外层执行可由行为继续；不应提前通知根结束 |
| 节点或监听器抛错 | 异常传给当前调用方，可能有部分写入/副作用；没有自动 FAILED 状态或事务回滚 |

上层如需“失败后终止”，可在确认适用时显式 `deleteCascade(reason)`，并处理取消本身再次抛错的可能性。取消成功状态在通知和清理成功后设置；不承诺重复取消幂等。结束监听器失败时根可能已经 ended=true，不能用再次 signal 作为通用恢复机制。包装类型及上层责任见 [异常传播契约](failure-propagation-contract.md)。

## 功能覆盖与当前限制

下表按能力分组。A 指独立 JAR 组合测试 `TextBatchAcceptanceTest`，B 指新增 `ApiUsageExamplesTest`；“引擎回归”是仓库已有测试，不冒充独立消费者覆盖。

| 能力/API | 实现和覆盖 | 边界 |
| --- | --- | --- |
| Builder、定义/活动/连线属性、只读模型查询 | A+B | 不承诺完整图验证或运行中改定义 |
| createProcessInstance、start、end、take | A+B | 同实例串行；不是持久化实例服务 |
| 条件选择、等待、signalData | A+B | 自定义行为；无消息系统或自动表达式路由 |
| takeAll、joined、逻辑并行汇合 | A 六种顺序、实例交错 | 本图全入边汇合；未覆盖任意循环/可选分支组合 |
| 嵌套 scope 和跨 scope 连线 | A | 更复杂收缩由既有 ScopePruningChildTreeTest 等保护 |
| CompositeActivityBehavior、executeActivity | B 单内部活动 | 未独立验证复合行为内多分支、中断的全部组合 |
| SubProcessActivityBehavior | A 输入、输出、回调、取消、异常 | 需显式传值；无自动继承或业务事务 |
| 值变量、Local、批量赋值、存在判断、类型 getter、删除 | A+B，VariableScopeContractTest | 全部重载并非都有独立消费者断言 |
| 严格创建与重复键拒绝 | 单键已有变量回归；批量实现已有回归 | createVariablesLocal、setVariableLocally、deleteVariablesLocal 仅实现类提供，未列入公开 VariableScope；案例不向实现类强转 |
| start/end/take 监听器、来源 | A+B | scope 路径通知非全局一次；没有事务事件总线 |
| deleteCascade、终态 signal 拒绝 | A+B | 不承诺重复取消幂等、旧子执行引用或取消失败回滚 |
| destroyScope、调用者继续 | 引擎回归 | 本次独立案例没有直接调用 |
| 同步异常传播、保留部分状态 | A；FailurePropagationContractTest | 独立案例注入 RuntimeException；checked exception 包装由引擎回归覆盖 |
| JsonModel、Node/Edge、SimpleModelParserImpl、行为注册 | B 平面两节点模型 | 解析器选择列表第一个节点为入口；只解析点线，不提供业务模型校验、嵌套 scope、条件规则或 JSON 文本解码服务 |
| IModelLoader | 仅接口 | 无内置模板存储/加载实现 |
| JavaDelegate、Expression、getSkipExpression | 扩展接口/属性存在 | 未发现自动调用 JavaDelegate 或内核求值 skipExpression 的路径；注册 ActivityBehavior 并显式编写路由 |
| VariableInstance 元数据 getter | ExecutionImpl 返回 null | 未实现，不作值变量 API 使用 |
| 租户和执行 ID | getTenantId 返回 null；内存实例未自动分配 ID | 宿主管理业务标识，不假设 getId 全局唯一 |
| delegate.event 下事件类型/监听接口 | 类型定义存在 | 未见连接到 PVM 的统一分发服务；使用上述 ExecutionListener |
| createExecution、setActive/setEnded/setScope/setConcurrent、执行树查询 | 底层扩展 API | 案例仅在网关使用必要操作；状态 setter 不代替生命周期操作，未穷举手工树变更 |
| 持久化、跨进程恢复、线程安全、自动重试、部署服务 | 不属于当前已实现目标 | 不通过类名、Serializable 或接口存在推断支持 |

当前检查未覆盖长期压力、全部故障组合、真实外部副作用及其他 JDK/操作系统。增加能力时应给覆盖表补充可执行证据；发现未明确语义时先定义契约，不通过局部补丁扩大承诺。
