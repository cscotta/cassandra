**Commit**: 5ae4b62876 · **Ticket**: KAFKA-18347
**Subsystem**: bin/kafka-run-class.sh
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  export MSYS2_ARG_CONV_EXCL="-Xlog:gc*:file=;-Dlog4j.configuration=;$MSYS2_ARG_CONV_EXCL"
  LOG4J_DIR="$base_dir/config/tools-log4j.properties"
  KAFKA_LOG4J_OPTS="-Dlog4j.configuration=file:${LOG4J_DIR}"
	set KAFKA_LOG4J_OPTS=-Dlog4j.configuration=file:%BASE_DIR%/config/tools-log4j.properties
log4j.rootLogger=WARN, stderr
log4j.appender.stderr=org.apache.log4j.ConsoleAppender
log4j.appender.stderr.layout=org.apache.log4j.PatternLayout
log4j.appender.stderr.layout.ConversionPattern=[%d] %p %m (%c)%n
log4j.appender.stderr.Target=System.err

// After
  export MSYS2_ARG_CONV_EXCL="-Xlog:gc*:file=;-Dlog4j2.configurationFile=;$MSYS2_ARG_CONV_EXCL"
  LOG4J_DIR="$base_dir/config/tools-log4j2.yaml"
  KAFKA_LOG4J_OPTS="-Dlog4j2.configurationFile=${LOG4J_DIR}"
	set KAFKA_LOG4J_OPTS=-Dlog4j2.configurationFile=file:%BASE_DIR%/config/tools-log4j2.yaml
Configuration:
  Properties:
    Property:
      - name: "logPattern"
        value: "[%d] %p %m (%c)%n"
  Appenders:
    Console:
      name: STDERR
```

**How to spot**
When reviewing bin/kafka-run-class.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18347 add tools-log4j2.yaml to config and remove unsed tools-log4j.properties from config (#18351).

**Test**
No test added.
