**Commit**: 7da9457b36 · **Ticket**: KAFKA-19260
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Move `LoggingController` to server module and rewrite it in java. Reviewers: PoAn Yang <payang@apache.org>, Ken Huang
 <s7133700@gmail.com>, Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.utils.LoggingController;

                    if (loggerName.equals(LoggingController.ROOT_LOGGER())) {
                            LoggingController.ROOT_LOGGER() + " logger is not allowed");
import kafka.utils.{LoggingController, Logging}
              createResponseConfig(LoggingController.loggers,


// After
    compileOnly libs.bndlib
    compileOnly libs.spotbugs

    implementation log4j2Libs
    <allow pkg="org.apache.logging.log4j" />
    <allow pkg="org.apache.logging.log4j.core" />
    <allow pkg="org.apache.logging.log4j.core.config" />
import org.apache.kafka.server.logger.LoggingController;
                    if (loggerName.equals(LoggingController.ROOT_LOGGER)) {
                            LoggingController.ROOT_LOGGER + " logger is not allowed");
import kafka.utils.Logging
import org.apache.kafka.server.logger.LoggingController
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19260 move loggingcontroller to server module (#19687).

**Test**
Test modified in RuntimeLoggerManagerTest.java covering the corrected behaviour.
