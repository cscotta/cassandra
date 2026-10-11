**Commit**: 77e952687b · **Ticket**: KAFKA-17559
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: David Arthur <mumrah@gmail.com>, Satish Duggana <satishd@apache.org>, Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
        rlm = new MockRemoteLogManager(2, 10, logDir.toString());
        // Task queue size is 10 and number of threads is 2, so it can accept at-most 12 items
        for (int i = 0; i < 12; i++) {
        AsyncOffsetReadFutureHolder<Either<Exception, Option<TimestampAndOffset>>> last = holderList.get(holderList.size() - 1);
        last.taskFuture().get(100, TimeUnit.MILLISECONDS);

        assertEquals(12, holderList.size());
        assertEquals(11, holderList.stream().filter(h -> h.taskFuture().isDone()).count());
        try (RemoteLogManager rlm = new MockRemoteLogManager(2, 10, logDir.toString()) {

// After
        rlm = new MockRemoteLogManager(2, 1, logDir.toString());
        // Task queue size is 1 and number of threads is 2, so it can accept at-most 3 items
        for (int i = 0; i < 3; i++) {
        for (AsyncOffsetReadFutureHolder<Either<Exception, Option<TimestampAndOffset>>> holder : holderList) {
            if (!holder.jobFuture().isCancelled()) {
                holder.taskFuture().get(1, TimeUnit.SECONDS);
            }
        }
        assertEquals(3, holderList.size());
        assertEquals(2, holderList.stream().filter(h -> h.taskFuture().isDone()).count());
        try (RemoteLogManager rlm = new MockRemoteLogManager(2, 1, logDir.toString()) {
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17559: fix the flaky remotelogoffsetreader#testtaskqueuefullandcanceltask (#17214).

**Test**
Test modified in RemoteLogOffsetReaderTest.java covering the corrected behaviour.
