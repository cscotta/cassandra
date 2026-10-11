**Commit**: 38ebb8f48a · **Ticket**: KAFKA-17270
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Exit.scala is a wrapper around Exit.java, and its main benefit is that it allows methods to throw exceptions by returning Nothing. This benefit is minimal, and since we are planning to phase out Scala code, it would be better to use Exit.java directly and remove Exit.scala. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.utils.{Exit, Logging}
import org.apache.kafka.common.utils.{Java, LoggingSignalHandler, OperatingSystem, Time, Utils}
      Exit.addShutdownHook("kafka-shutdown-hook", {
import org.apache.kafka.common.utils.{Utils, SecurityUtils => JSecurityUtils}
import kafka.utils.{Exit, Logging}
import org.apache.kafka.common.utils.{Sanitizer, Time, Utils}
import kafka.utils.{Exit, Logging, ToolsUtils}
import org.apache.kafka.common.utils.{Time, Utils}
import kafka.utils.{Exit, Logging}

// After
import kafka.utils.Logging
import org.apache.kafka.common.utils.{Exit, Java, LoggingSignalHandler, OperatingSystem, Time, Utils}
      Exit.addShutdownHook("kafka-shutdown-hook", () => {
import org.apache.kafka.common.utils.{Exit, Utils, SecurityUtils => JSecurityUtils}
import kafka.utils.Logging
import org.apache.kafka.common.utils.{Exit, Sanitizer, Time, Utils}
import kafka.utils.{Logging, ToolsUtils}
import org.apache.kafka.common.utils.{Exit, Time, Utils}
import kafka.utils.Logging
import org.apache.kafka.common.utils.Exit
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17270 let test code use exit.java rather than exit.scala (#16821).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
