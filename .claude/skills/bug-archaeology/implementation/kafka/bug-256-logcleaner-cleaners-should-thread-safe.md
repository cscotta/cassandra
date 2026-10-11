**Commit**: f766ce54e6 · **Ticket**: KAFKA-20346
**Subsystem**: storage/src
**Category**: Race condition / missing synchronization

**Root cause**
logCleaner#cleaner can be accessed by two threads `
kafka-0-metadata-loader-event-handler` and JMX reader thread so we
should  use copyOnwrtieList to avoid concurrent issues. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.ArrayList;
    private final List<CleanerThread> cleaners = new ArrayList<>();

// After
import java.util.concurrent.CopyOnWriteArrayList;
    private final List<CleanerThread> cleaners = new CopyOnWriteArrayList<>();
import java.util.concurrent.CompletableFuture;
    @Test
    public void testGaugeReadsAreNotAffectedByReconfigure() throws Exception {
        cleaner = makeCleaner(TOPIC_PARTITIONS, CLEANER_BACKOFF_MS, MIN_COMPACTION_LAG, SEGMENT_SIZE);
        cleaner.startup();

        AbstractConfig config1Thread = makeReconfigureConfig(1);
        AbstractConfig config2Thread = makeReconfigureConfig(2);

        var checkError = CompletableFuture.runAsync(() -> {
```

**How to spot**
When reviewing storage/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20346 logcleaner#cleaners should be thread-safe (#21847).

**Test**
Test modified in LogCleanerIntegrationTest.java covering the corrected behaviour.
