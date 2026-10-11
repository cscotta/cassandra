**Commit**: 30edb584cc · **Ticket**: KAFKA-20323
**Subsystem**: gradle/dependencies.gradle
**Category**: Logic error in condition

**Root cause**
`testRecordPruningTaskPeriodicityWithSomeFailures` fails with Mockito
5.21+ because  unstubbed `CompletableFuture` methods now
return`completedFuture(null)` instead of `null`, so unstubbed
`deleteRecords` calls succeed instead of being no-ops. Changed `deleteRecords` stubs from offset-based matching to
partition-based matching, which better reflects the test intent (tp1
succeeds, tp2 fails) and removes dependency on Mockito's default return
behavior. Upgraded Mockito from 5.20.0 to 5.23.0 as the issue description allows
5.21.0 or more recent versions. Verified locally that both 5.21.0 and
5.23.0 pass all `ShareCoordinatorServiceTest` tests.

**Before/After**
```java
// Before
  mockito: "5.20.0",
            any(),
            eq(10L)
            any(),
            eq(20L)

// After
  mockito: "5.23.0",
            eq(tp1),
            anyLong()
            eq(tp2),
            anyLong()
```

**How to spot**
When reviewing gradle/dependencies.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20323: refactor deleterecords stub to use partition-based matching for mockito upgrade (#21857).

**Test**
Test modified in ShareCoordinatorServiceTest.java covering the corrected behaviour.
