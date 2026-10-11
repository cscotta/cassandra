**Commit**: 709c5fab22 · **Ticket**: KAFKA-19666
**Subsystem**: streams/integration-tests
**Category**: State not cleaned up

**Root cause**
clean up `EosIntegrationTest.java`

Reviewers: Lucas Brutschy <lucasbru@apache.org>

**Before/After**
```java
// Before
    public void shouldCheckpointRestoredOffsetsWhenClosingCleanDuringRestoringStateUpdaterEnabled(
        shouldCheckpointRestoredOffsetsWhenClosingCleanDuringRestoring(processingThreadsEnabled, true);
    }

    @Test
    public void shouldCheckpointRestoredOffsetsWhenClosingCleanDuringRestoringStateUpdaterDisabled() throws Exception {
        shouldCheckpointRestoredOffsetsWhenClosingCleanDuringRestoring(false, false);
    }

    private void shouldCheckpointRestoredOffsetsWhenClosingCleanDuringRestoring(
            final boolean processingThreadsEnabled,
            final boolean stateUpdaterEnabled) throws Exception {

// After
    public void shouldCheckpointRestoredOffsetsWhenClosingCleanDuringRestoring(
```

**How to spot**
When reviewing streams/integration-tests code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19666: remove old restoration codepath from eosintegrationtest [5/n] (#20499).

**Test**
Test modified in EosIntegrationTest.java covering the corrected behaviour.
