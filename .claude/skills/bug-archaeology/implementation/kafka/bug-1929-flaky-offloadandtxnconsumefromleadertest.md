**Commit**: 65fb274d29 · **Ticket**: KAFKA-17998
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Satish Duggana <satishd@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.common.test.api.Flaky;
import static org.apache.kafka.tiered.storage.specs.RemoteFetchCount.OperationType.EQUALS_TO;
@Flaky("KAFKA-17998")
        FetchCountAndOp segmentFetchCountAndOp = new FetchCountAndOp(6, EQUALS_TO);

// After
        // Ideally, each remote-log segment should be fetched only once. For 6 segments, we would have 6 fetch-counts.
        // But, the client can retry the FETCH request, to make the test deterministic, increasing the fetch-count
        // to be at-max of 12 (2 times of fetch-count).
        FetchCountAndOp segmentFetchCountAndOp = new FetchCountAndOp(12, LESS_THAN_OR_EQUALS_TO);
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17998: fix the flaky offloadandtxnconsumefromleadertest (#17959).

**Test**
Test modified in OffloadAndTxnConsumeFromLeaderTest.java covering the corrected behaviour.
