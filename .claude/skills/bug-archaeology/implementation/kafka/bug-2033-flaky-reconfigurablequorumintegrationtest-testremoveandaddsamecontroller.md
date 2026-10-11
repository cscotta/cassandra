**Commit**: 4fcb8b247e · **Ticket**: KAFKA-17988
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.test.api.Flaky;
    @Flaky("KAFKA-17988")

// After
                TestUtils.retryOnExceptionWithTimeout(30_000, 10, () -> {
                    Map<Integer, Uuid> voters = findVoterDirs(admin);
                    assertEquals(new HashSet<>(Arrays.asList(3001, 3002, 3003)), voters.keySet());
                    for (int replicaId : new int[] {3001, 3002, 3003}) {
                        assertNotEquals(Uuid.ZERO_UUID, voters.get(replicaId));
                    }
                });
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17988: fix flaky reconfigurablequorumintegrationtest.testremoveandaddsamecontroller (#17782).

**Test**
Test modified in ReconfigurableQuorumIntegrationTest.java covering the corrected behaviour.
