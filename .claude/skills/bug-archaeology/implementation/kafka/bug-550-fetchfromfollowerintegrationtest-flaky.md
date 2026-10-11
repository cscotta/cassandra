**Commit**: b9d6adda22 · **Ticket**: KAFKA-15700
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
# Description
This PR fixes the flakiness in `testRackAwareRangeAssignor`. Currently,
it fails during the verification after `alterPartitionReassignments`,
and also during `groupProtocol = consumer`. ## Using CLASSIC Group Protocol
The test relies on auto-commit, which is not deterministic during the
rebalance triggered by `alterPartitionReassignments`. Consequently, the
consumer may fail to commit offsets before the rebalance completes.

**Before/After**
```java
// Before
import org.junit.jupiter.api.{Disabled, Timeout}
import org.junit.jupiter.params.provider.MethodSource
  @Disabled
  @MethodSource(Array("getTestGroupProtocolParametersAll"))

// After
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.provider.{MethodSource, ValueSource}
  @ValueSource(strings = Array("classic"))
      consumerConfig.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
      consumers.foreach{ _.commitSync() }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15700: fetchfromfollowerintegrationtest is flaky (#20982).

**Test**
Test modified in FetchFromFollowerIntegrationTest.scala covering the corrected behaviour.
