**Commit**: 7b99ee29a5 · **Ticket**: KAFKA-19346
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
1. Move `LogReadResult` to server module. 2. Rewrite `LogReadResult` in Java.

**Before/After**
```java
// Before
import kafka.server.LogReadResult;
                                    logReadResult.lastStableOffset().isDefined() ? OptionalLong.of((Long) logReadResult.lastStableOffset().get()) : OptionalLong.empty(),
                                    logReadResult.preferredReadReplica().isDefined() ? OptionalInt.of((Integer) logReadResult.preferredReadReplica().get()) : OptionalInt.empty(),
import kafka.server.LogReadResult;

            if (result.lastStableOffset.isDefined) OptionalLong.of(result.lastStableOffset.get) else OptionalLong.empty(),
            if (result.preferredReadReplica.isDefined) OptionalInt.of(result.preferredReadReplica.get) else OptionalInt.empty(),
import org.apache.kafka.common.message.{DescribeLogDirsResponseData, DescribeProducersResponseData, FetchResponseData}

// After
import org.apache.kafka.server.LogReadResult;
                                    logReadResult.lastStableOffset().isPresent() ? OptionalLong.of(logReadResult.lastStableOffset().getAsLong()) : OptionalLong.empty(),
                                    logReadResult.preferredReadReplica().isPresent() ? OptionalInt.of(logReadResult.preferredReadReplica().getAsInt()) : OptionalInt.empty(),
import org.apache.kafka.server.LogReadResult;
import org.apache.kafka.server.LogReadResult
            if (result.lastStableOffset.isPresent) OptionalLong.of(result.lastStableOffset.getAsLong) else OptionalLong.empty(),
            if (result.preferredReadReplica.isPresent) OptionalInt.of(result.preferredReadReplica.getAsInt) else OptionalInt.empty(),
import org.apache.kafka.common.message.{DescribeLogDirsResponseData, DescribeProducersResponseData}
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19346: move logreadresult to server module (#19846).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
