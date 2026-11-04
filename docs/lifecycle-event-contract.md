# 取消与流程结束通知的归属

本契约针对同一实例串行驱动，补充现有生命周期测试；不定义全局事件去重或取消后的旧对象状态。

## 明确的边界

- 活动/scope 的 end 和流程定义的 end 是不同层级的事件来源。流程定义的 end 只由所属流程的根执行器发出，不能由普通 scope 子执行器代发。
- `destroyScope` 清理后代（以及自身调用的子流程），保留调用者供显式继续。清理普通 scope 不能通知仍存活的所属流程结束。
- `deleteCascade` 取消整个流程时，仍保留该流程根的 end 通知。调用式子流程有自己的根与流程定义，被取消时仍通知自己的流程 end，但不调用正常完成的 completing/completed 回调。此行为沿用 PR #6。
- 正常完成仍由 PROCESS_END 在根上设置终态并通知流程定义。取消通知不自动等同于正常完成，也不新增取消对象 ended/active 状态承诺。

## 根因与修复范围

取消分发的 getScope 在 activity 为 null 时递归查父执行器，最终可能返回流程定义。以前每个清理到该阶段的 scope 子执行器都通知流程定义，导致 destroyScope 提前通知、完整 deleteCascade 重复通知。

现在仅跳过非根执行器误选到的流程定义通知，随后仍执行既有清理和级联；真正根的通知不变。没有修改正常完成、takeAll、执行树剪枝或子流程关联。

PR #5 已定义的路径相关 scope 通知仍原样保留（例如两条路径分别离开同一 scope 可以分别通知）；本次不把所有 scope end 改成全局一次，也不重写活动事件所属执行器的现有规则。

## 可执行验证与未承诺项

`CancellationProcessEndOwnerTest` 覆盖单层/三层 scope 清理后的继续运行、整流程取消、无 scope 取消、自然完成对照，以及带嵌套 scope 的调用式子流程取消；检查流程事件的执行器身份与通知次数，并保留活动/scope 通知。

`DestroyScopeContractTest`、`ConcurrentScopeEndNotificationTest`、`NaturalScopeCompletionTest` 和 `SubProcessReplacementTest` 保护既有契约。这里的通知次数针对一次无异常操作，不承诺重复取消幂等性、监听器副作用的 exactly-once、异常回滚或多线程安全。
