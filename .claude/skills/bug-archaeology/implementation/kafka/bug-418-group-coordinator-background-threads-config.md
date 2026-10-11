**Commit**: 065315431d · **Ticket**: KAFKA-20210
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Add the group.coordinator.background.threads config option to control
the  number of threads in the group coordinator's CoordinatorExecutor
thread  pool. Reviewers: Dongnuo Lyu <dlyu@confluent.io>, Chia-Ping Tsai
 <chia7712@gmail.com>, PoAn Yang <payang@apache.org>, David Jacot
 <djacot@confluent.io>

**Before/After**
```java
// Before
    public static final String GROUP_COORDINATOR_NUM_THREADS_DOC = "The number of threads used by the group coordinator.";

// After
        case GroupCoordinatorConfig.GROUP_COORDINATOR_NUM_BACKGROUND_THREADS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
    public static final String GROUP_COORDINATOR_NUM_THREADS_DOC = "The number of threads used by the group coordinator for processing requests.";
    public static final String GROUP_COORDINATOR_NUM_BACKGROUND_THREADS_CONFIG = "group.coordinator.background.threads";
    public static final String GROUP_COORDINATOR_NUM_BACKGROUND_THREADS_DOC = "The number of threads used by the group coordinator for " +
        "processing background tasks (e.g. updating regular expression subscriptions and offloaded assignments).";
    public static final int GROUP_COORDINATOR_NUM_BACKGROUND_THREADS_DEFAULT = 2;

        .define(GROUP_COORDINATOR_NUM_BACKGROUND_THREADS_CONFIG, INT, GROUP_COORDINATOR_NUM_BACKGROUND_THREADS_DEFAULT, atLeast(1), HIGH, GROUP_COORDINATOR_NUM_BACKGROUND_THREADS_DOC)
    private final int numBackgroundThreads;
        this.numBackgroundThreads = config.getInt(GroupCoordinatorConfig.GROUP_COORDINATOR_NUM_BACKGROUND_THREADS_CONFIG);
    /**
     * The number of background threads.
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20210: add group.coordinator.background.threads config (#21555).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
