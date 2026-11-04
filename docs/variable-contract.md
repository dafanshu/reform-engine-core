# ExecutionImpl 内存变量契约

本契约针对嵌入式 PVM，同一实例及其子执行树由调用者串行驱动。它不提供线程安全、持久化、事务回滚或业务结果聚合。契约测试见 `VariableScopeContractTest`；剪枝兼容性另由 `LocalVariablePruningTest` 保护。

## 查找、写入与隔离

- 每个流程实例独立保存变量 map；共享同一流程定义不会共享实例变量。
- `getVariable` 沿 parent 链取最近绑定；`setVariable` 更新最近已有绑定，没有绑定则在父链根创建。不会沿 superExecution 查找调用者变量。
- `setVariableLocal`、`setVariableLocally` 只写当前执行，首次调用也必须可用。`setVariableLocal` 保持既有 null 返回值，不承诺返回旧值或 VariableInstance；调用者用 getter 读取结果。
- `getVariableLocal`、`hasVariableLocal`、`hasVariablesLocal` 只看当前执行；非 Local 存在判断含祖先。存在 null 值的键仍算存在，并遮蔽父层同名值。
- `setVariablesLocal` 合并本地 map；null map 保持既有无操作行为。map、名称集合的查询结果是容器快照；选定名称查询沿用既有 `getVariables(names)` 规则：缺失名称也返回一个 null 值条目。Local 查询不读取祖先。
- boolean `fetchAllVariables` 重载在内存实现中不改变语义；类型 getter 使用 Class.cast。
- 值对象不深拷贝。两个实例若由调用者传入同一个可变对象，修改该对象会同时可见；隔离的是绑定容器，不是对象引用。

## 删除

- `removeVariable(name)` 删除最近存在的绑定；删掉子层遮蔽后，父层值重新可见。不存在则无操作。
- `removeVariableLocal` 只删除本地；集合版本逐个执行对应操作。
- `removeVariables()` 先取得当前可见名称快照，然后每个名称删除一次最近绑定。因此可能重新露出被遮蔽的父层值；它不是沿祖先递归清空所有同名绑定。
- `removeVariablesLocal()` / `deleteVariablesLocal()` 清空本地容器，保留祖先和子执行的容器。
- 集合参数按现有选定名称查询方式要求非 null，本轮不增加 null 集合的特殊语义。

## 生命周期及现有迁移行为

- 自然结束后，仍持有的根实例可读取其变量。异常发生前的变量写入保留；恢复由调用者显式执行，不回滚业务状态。
- 调用式子流程有独立根。输入由调用行为显式赋值，输出在 completing 中显式复制；正常 completed 推进调用者。没有自动继承或自动合并。
- 剪枝把幸存执行的局部绑定合并进父执行，幸存值覆盖父执行同名绑定。两种分支结束顺序可能留下不同结果，这是既有行为，本次未改动。
- 已结束分支的局部值不自动成为聚合结果；仍需应用显式写入约定的数据位置。不能把局部迁移当作顺序无关的并行归并。
- 本轮不改变 takeAll、跨 scope 剪枝、子流程关联或取消事件语义。

## 尚未纳入支持契约

`VariableInstance` 系列属于未实现的元数据/持久化扩展接口，不在本次内存值 API 修复范围。
`createVariableLocal` / `createVariablesLocal` 仍是原有占位接口；现有代码及测试未定义同名键已存在时应拒绝还是覆盖，也没有生产调用点。本轮不替调用者决定：若要支持严格创建，应约定重复键异常以及批量创建是否先整体校验；若要支持覆盖，应明确它与 set 的别名关系。当前请使用已实现的 set 系列。
