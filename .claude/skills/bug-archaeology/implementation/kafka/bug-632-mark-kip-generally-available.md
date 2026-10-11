**Commit**: d9d9fcb705 · **Ticket**: KAFKA-19757
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This is the first PR to mark KIP-932 as generally available. It removes
warning messages, updates the javadoc and removes "Preview" from the
docs. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
 * <p>
 *     <em>This is a preview feature introduced by KIP-932. It is not yet recommended for production use.</em>
import org.slf4j.Logger;

            LogContext logContext = new LogContext();
            Logger log = logContext.logger(getClass());
            log.warn("Share groups and KafkaShareConsumer are part of a preview feature introduced by KIP-932, and are not recommended for use in production.");
            Logger log = logContext.logger(getClass());
            log.warn("Share groups and KafkaShareConsumer are part of a preview feature introduced by KIP-932, and are not recommended for use in production.");
	<h3 class="anchor-heading"><a id="shareconsumerapi" class="anchor-link"></a><a href="#shareconsumerapi">2.3 Share Consumer API (Preview)</a></h3>
	The Share Consumer API (Preview) enables applications within a share group to cooperatively consume and process data from Kafka topics.
    Share groups are available as a preview in Apache Kafka 4.1.

// After
	<h3 class="anchor-heading"><a id="shareconsumerapi" class="anchor-link"></a><a href="#shareconsumerapi">2.3 Share Consumer API</a></h3>
	The Share Consumer API enables applications within a share group to cooperatively consume and process data from Kafka topics.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19757: [1/n] mark kip-932 as generally available (#20856).

**Test**
No test added.
