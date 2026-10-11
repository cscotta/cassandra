**Commit**: 553e6b4c6d · **Ticket**: KAFKA-17860
**Subsystem**: .github/configs
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    - 'log4j-appender/**'
    // log4j-appender is not in core dependencies,
    // so we add it to dependant-testlibs to avoid ClassNotFoundException in running kafka_log4j_appender.py
    from (project(':log4j-appender').jar ) { "$buildDir/dependant-testlibs" }
project(':log4j-appender') {
  base {
    archivesName = "kafka-log4j-appender"
  }

  dependencies {
    implementation project(':clients')
    implementation libs.slf4jReload4j

// After
                <li>
                    KafkaLog4jAppender has been remove, users should migrate to the log4j2 appender
                    See <a href="https://logging.apache.org/log4j/2.x/manual/appenders.html#KafkaAppender">KafkaAppender</a> for more details
                </li>
```

**How to spot**
When reviewing .github/configs code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17860 remove log4j-appender module (#17588).

**Test**
Test modified in KafkaLog4jAppenderTest.java covering the corrected behaviour.
