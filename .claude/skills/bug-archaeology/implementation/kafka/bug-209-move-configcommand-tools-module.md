**Commit**: 46773ce200 · **Ticket**: KAFKA-14588
**Subsystem**: bin/kafka-configs.sh
**Category**: Logic error in condition

**Root cause**
Rewrite ConfigCommand in Java and move it to the tools module

Reviewers: Ken Huang <s7133700@gmail.com>, Christo Lolov
 <lolovc@amazon.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
exec $(dirname $0)/kafka-run-class.sh kafka.admin.ConfigCommand "$@"
"%~dp0kafka-run-class.bat" kafka.admin.ConfigCommand %*
    <allow pkg="kafka.admin" />
  <subpackage name="admin">
    <allow pkg="kafka.admin"/>
    <allow pkg="kafka.cluster"/>
    <allow pkg="kafka.log"/>
    <allow pkg="kafka.server"/>
    <allow pkg="org.apache.kafka.coordinator.group"/>
    <allow pkg="org.apache.kafka.metadata"/>
    <allow pkg="org.apache.kafka.security"/>
    <allow pkg="org.apache.kafka.storage.internals.checkpoint"/>

// After
exec $(dirname $0)/kafka-run-class.sh org.apache.kafka.tools.ConfigCommand "$@"
"%~dp0kafka-run-class.bat" org.apache.kafka.tools.ConfigCommand %*
    <allow pkg="org.apache.kafka.server.metrics" />
              files="(AclCommand|ConfigCommand|ConsoleConsumer|DefaultMessageFormatter|StreamsResetter|ProducerPerformance|Agent).java"/>
              files="(AclCommand|ConfigCommand|DefaultMessageFormatter|ProducerPerformance|StreamsResetter|Agent|TransactionalMessageCopier|ReplicaVerificationTool|LineMessageReader|ConsoleProducer|DumpLogSegments).java"/>
```

**How to spot**
When reviewing bin/kafka-configs.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14588 move configcommand to tools module (#22013).

**Test**
Test modified in ClientTelemetryTest.java covering the corrected behaviour.
