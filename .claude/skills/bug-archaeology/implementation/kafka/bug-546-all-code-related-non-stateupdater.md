**Commit**: 73dc373714 · **Ticket**: KAFKA-18913
**Subsystem**: committer-tools/README.md
**Category**: Logic error in condition

**Root cause**
This PR cleans up all non stateupdater code. We also remove the config
`__state.updater.enabled__` which means users will no longer have the
option to disable the stateupdater. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
2024-09-10T20:31:26.6830206Z Gradle Test Run :streams:test > Gradle Test Executor 47 > StreamThreadTest > shouldReturnErrorIfProducerInstanceIdNotInitialized(boolean, boolean) > "shouldReturnErrorIfProducerInstanceIdNotInitialized(boolean, boolean).stateUpdaterEnabled=true, processingThreadsEnabled=true" STARTED
        // Private API to enable the state updater (i.e. state updating on a dedicated thread)
        public static final String STATE_UPDATER_ENABLED = "__state.updater.enabled__";

        public static boolean stateUpdaterEnabled(final Map<String, Object> configs) {
            return true; // always enabled
        }

    private final ChangelogReader storeChangelogReader;
    private final boolean stateUpdaterEnabled;
                      final ChangelogReader storeChangelogReader,
                      final boolean stateUpdaterEnabled,

// After
2024-09-10T20:31:26.6830206Z Gradle Test Run :streams:test > Gradle Test Executor 47 > StreamThreadTest > shouldReturnErrorIfProducerInstanceIdNotInitialized(boolean) > "shouldReturnErrorIfProducerInstanceIdNotInitialized(boolean).processingThreadsEnabled=true" STARTED
                partitions);
```

**How to spot**
When reviewing committer-tools/README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18913: remove all code related to non-stateupdater path (#21059).

**Test**
Test modified in ActiveTaskCreatorTest.java covering the corrected behaviour.
