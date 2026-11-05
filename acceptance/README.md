# 独立 JAR 发布前检查

这是虚构文本批处理场景的内核集成检查，不是真实业务验收，也不证明所有流程图或自定义节点都正确。消费者只通过正式构建的引擎 JAR 和公开 PVM/delegate API 运行，未使用仓库测试节点或 runtime 实现类。

## 环境和运行

使用 Bash（Linux/macOS）、JDK 11 和 Maven；本次验证版本为 Temurin 11.0.29、Maven 3.9.11。设置 `JAVA_HOME`，使 `mvn` 可用；首次运行需要访问 Maven 仓库下载依赖。

在仓库根目录执行：

```sh
export JAVA_HOME=/path/to/jdk-11
bash acceptance/run.sh
```

可用 `MVN=/path/to/mvn` 指定 Maven。脚本参数原样传给两次 Maven 调用，例如：

```sh
bash acceptance/run.sh -s /path/to/settings.xml -Dmaven.repo.local=/path/to/local-repository
```

参数仅用于 Maven 环境配置；不要传入跳过测试的选项。代理、仓库认证和证书信任由宿主 Maven 配置管理，不写入工程。脚本不依赖启动时的工作目录，但参数中的相对路径相对于调用目录。

检查依次执行：

1. 引擎 `clean install`：运行全部引擎测试，构建正式 JAR，并安装到本地 Maven 仓库。
2. 独立消费者 `clean verify dependency:build-classpath`：普通 Maven 依赖解析该 JAR，运行验收测试并生成仅运行依赖的 classpath。没有 parent、reactor、测试 JAR 或引擎源码目录依赖。
3. 使用 `JAVA_HOME/bin/java`，仅以消费者 JAR、引擎 JAR 和运行依赖启动示例。任一步失败即非零退出；不会推送、deploy 或触发发布。

测试要求消费者加载的 JAR 与本次引擎 `target` 中的产物逐字节一致，并确认仓库测试类 `pvm.ucase.behavior.ParallelGateway` 不可加载，避免误验旧 SNAPSHOT 或测试 classpath。单独执行消费者测试需要提供 `-Dacceptance.engineJar=/absolute/path/to/fresh-engine.jar`；建议使用完整脚本。

消费者依赖当前引擎 `1.0-SNAPSHOT`；引擎版本改变时应同步修改 `consumer/pom.xml` 的依赖版本。引擎运行依赖仍为 commons-lang3 3.4、slf4j-api 1.7.25、fastjson 1.2.59；消费者的 JUnit 4.12 仅用于测试，没有升级引擎依赖。未配置日志后端时 SLF4J 提示使用 NOP logger，流程仍可运行；宿主可自行选择兼容后端。

## 场景和断言

所有等待、条件路由、并行网关、处理任务及调用式子流程行为都是消费者自定义代码，不是引擎内置业务节点。信号为直接 Java 调用，“并行”表示执行树分支，驱动调用始终串行。

```text
submit → choose ┬→ rejected（空批次或包含无效文本）
                ├→ quick → publish（单条）
                └→ processingScope（多条）
                   fork ┬→ normalizationScope / normalize ─┐
                        ├→ classify ───────────────────────┤→ join → publish（scope 外）
                        └→ validationCall ─────────────────┘
                           └→ validationScope / validate
```

子流程输入 records 显式复制，valid 在 completing 回调显式返回，completed 推进调用节点。临时局部变量创建、读取后删除；批次结果保存在实例变量中。示例 publish 只写内存，不涉及外部事务。

九项测试覆盖：JAR 来源与测试类隔离；三分支全部六种完成顺序；单条快速路径；无效输入拒绝路径；共享定义的两个实例串行交错；节点失败及上层取消；子流程失败；产出阶段失败；结束监听器失败。断言包含收齐分支前不得产出、回调及结束事件次数、结果和实例隔离、结束后 signal 拒绝。对异常保留之前的数据和状态，不假设事务回滚；即使已经有 result 且 ended，驱动调用抛错仍应由上层按失败处理。空批次拒绝条件存在于示例，但当前拒绝测试使用空白文本。

普通成功运行的末行应为：

```text
RESULT=[ALPHA, BETA] STATUS=accepted ENDED=true END_EVENTS=1
```

## 边界和维护

本检查补充现有变量、生命周期和异常契约测试，不替代它们。尚未覆盖同实例多线程、长期压力、全部异常组合、真实外部副作用、其他 JDK/操作系统以及任意图结构；不增加持久化、恢复、重试或自动事务。子实例映射只是演示的定位方式，不是通用任务注册中心。

只执行根目录 `mvn test` 不会运行独立消费者。发布前应显式运行此脚本并保留结果；当前未接入 CI 或发布自动化。报告位于根目录和 `consumer/target/surefire-reports`，构建输出已忽略，不应提交。
