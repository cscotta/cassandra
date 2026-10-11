**Commit**: 1132f08c57 · **Ticket**: KAFKA-18773
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
- update reflection-config.json and resource-config.json to include log4j2 and jackson
- remove unused jackson scala library
- fix the incorrect path of log4j2.yaml
- adopt workaround (--standalone) to make this PR work and it will be fixed by KAFKA-18737)

Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    implementation libs.jacksonModuleScala
    Array("format", "--cluster-id=" + clusterId, "-c", s"${configsPath.toString}/server.properties")
    if (config == null && loggerFromEnv.isEmpty && rootOption.isEmpty) {
      return
    }

    val expected = Array("format", "--cluster-id=MYwKGPhXQZidgd0qMv8Mkw", "-c", "/path/to/configs/server.properties")
  - `KAFKA_LOG4J_ROOT_LOGLEVEL` can be provided to set the value of `log4j.rootLogger` in log4j.properties and `tools-log4j.properties`.
  - log4j loggers can be added to log4j.properties by setting them in `KAFKA_LOG4J_LOGGERS` environment variable in a single comma separated string.
          - `log4j.logger.property1=value1` and `log4j.logger.property2=value2` will be added to the `log4j.properties` file inside Docker container.

// After
    Array("format", "--cluster-id=" + clusterId, "-c", s"${configsPath.toString}/server.properties", "--standalone")
    val expected = Array("format", "--cluster-id=MYwKGPhXQZidgd0qMv8Mkw", "-c", "/path/to/configs/server.properties", "--standalone")
  - `KAFKA_LOG4J_ROOT_LOGLEVEL` can be provided to set the value of `log4j.rootLogger` in `log4j2.yaml` and `tools-log4j2.yaml`.
  - log4j loggers can be added to `log4j2.yaml` by setting them in `KAFKA_LOG4J_LOGGERS` environment variable in a single comma separated string.
          - `log4j.logger.property1=value1` and `log4j.logger.property2=value2` will be added to the `log4j2.yaml` file inside Docker container.
To update reachability metadata, users can set following environment variable and run related jar files. New metadata configs will be generated in the `native-image-configs` directory.
```bash
export JAVA_TOOL_OPTIONS="-agentlib:native-image-agent=config-merge-dir=/path/to/kafka/docker/native/native-image-configs"
```

```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18773 migrate the log4j1 config to log4j 2 for native image and readme (#18872).

**Test**
Test modified in KafkaDockerWrapperTest.scala covering the corrected behaviour.
