**Commit**: 33abb655eb · **Ticket**: KAFKA-19215
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
### About
Added code to handle share partition fetch lock cleanly in
`DelayedShareFetch` to avoid a member incorrectly releasing a share
partition's fetch lock

### Testing
The code has been tested with the help of unit tests and integration
tests. Reviewers: Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
            Optional.empty()
        Optional<PendingRemoteFetches> pendingRemoteFetchesOpt
            if (sharePartition.maybeAcquireFetchLock()) {
                        sharePartition.releaseFetchLock();
                        log.trace("Record lock partition limit exceeded for SharePartition {}, " +
                            "cannot acquire more records", sharePartition);
                    log.error("Error checking condition for SharePartition: {}", sharePartition, e);
                    sharePartition.releaseFetchLock();
            sharePartition.releaseFetchLock();

// After
import org.apache.kafka.common.Uuid;
    /**
     * fetchId serves as a token while acquiring/releasing share partition's fetch lock.
     */
    private final Uuid fetchId;
            Optional.empty(),
            Uuid.randomUuid()
        Optional<PendingRemoteFetches> pendingRemoteFetchesOpt,
        Uuid fetchId
        this.fetchId = fetchId;
            if (sharePartition.maybeAcquireFetchLock(fetchId)) {
                    log.trace("Fetch lock for share partition {}-{} has been acquired by {}", shareFetch.groupId(), topicIdPartition, fetchId);
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19215: handle share partition fetch lock cleanly using tokens (#19598).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
