# engine-core

工作流核心，这里只提供流程定义实现类；另外就是行为和执行器的上层接口，接口的实现由具体的工作流服务提供。

#### 最佳实践与功能覆盖

从 [嵌入式流程最佳实践](docs/best-practices.md) 开始：公开 API 的使用方式、可运行案例、功能覆盖矩阵和当前限制。

#### 不依赖数据库的运行示例

示例入口：`src/test/java/example/InMemoryWorkflowExample.java`，可在 IDE 中直接运行 `main`。
示例放在测试源码目录，不会打入生产 JAR；无需数据库、表结构或应用服务。

在项目根目录执行（JDK 8 或兼容版本，已验证 JDK 11）：

```shell
mvn test-compile org.codehaus.mojo:exec-maven-plugin:3.1.0:java "-Dexec.mainClass=example.InMemoryWorkflowExample" "-Dexec.classpathScope=test"
```

首次运行需要下载 Maven 依赖及运行插件。以下是示例输出，引擎还会输出调试日志：

```text
[1/4] 启动流程，requestId=DEMO-001
[2/4] 等待信号，active=[wait], ended=false
[3/4] 接收信号，name=continue, data=approved
[4/4] 流程结束，active=[], ended=true, result=approved
```

流程结构为 `start -> wait --continue--> end`：

1. 创建流程定义和内存实例，设置业务变量后调用 `start()`。
2. 等待节点的 `execute()` 不推进流程，`start()` 返回；此时实例仍未结束，也没有线程休眠或轮询。
3. 程序保留实例引用，模拟后续回调调用 `findExecution("wait").signal("continue", "approved")`。
4. 信号处理器保存结果并走到结束节点；结束后活动节点为空，`isEnded()` 为 `true`。

这里的信号是直接 Java 方法调用，不是网络消息。示例只接受 `continue`，错误信号会抛异常并保持等待。
所有状态和变量仅存于当前进程；进程退出后不能自动恢复，结束后的实例收到信号会抛出 PvmException，拒绝再次执行节点。

只验证此示例：

```shell
mvn "-Dtest=InMemoryWorkflowExampleTest" test
```

#### 独立 JAR 发布检查

在 JDK 11、Maven 环境中执行 `bash acceptance/run.sh`。检查会完整构建引擎，再单独构建消费者并运行流程测试与无测试依赖的 JAR 示例。虚构场景、自定义节点、参数和适用范围见 [验收说明](acceptance/README.md)。这是一项本地发布前检查，不会发布制品。

#### 打包发布
```
mvn clean source:jar package deploy -e
```

#### 一期开发支持的模板类型
```
#串行流程
/**
   * +-------+   +------+   +-----+
   * | start |-->| task |-->| end |
   * +-------+   +------+   +-----+
   */
```

```
#并行流程
/**
   *                   +----+
   *              +--->|end1|
   *              |    +----+
   *              |
   * +-----+   +----+
   * |start|-->|fork|
   * +-----+   +----+
   *              |
   *              |    +----+
   *              +--->|end2|
   *                   +----+
   */

/**
   *                      +---------+
   *                      | +----+  |
   *                  +---->| c1 |------+
   *                  |   | +----+  |   v
   * +-------+   +------+ |         | +------+   +-----+
   * | start |-->| fork | |         | | join |-->| end |
   * +-------+   +------+ |         | +------+   +-----+
   *                  |   | +----+  |   ^
   *                  +---->| c2 |------+
   *                      | +----+  |
   *                      +---------+
   */
```

```
#子流程
/**
    *           +------------------------------+
    * +-----+   | +-----------+   +----------+ |   +---+
    * |start|-->| |startInside|-->|endInsdide| |-->|end|
    * +-----+   | +-----------+   +----------+ |   +---+
    *           +------------------------------+
    */
```
#### 异常处理

节点、监听器或子流程完成回调失败时，异常同步传播到当前驱动调用方，由上层决定如何处理；不会自动重试或回滚。`isEnded()` 不是业务成功标志，结束监听器抛异常时实例可能已经进入终态。异常包装类型、部分执行状态和上层责任见 [异常传播契约](docs/failure-propagation-contract.md)。
