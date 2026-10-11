**Commit**: adb76779ed · **Ticket**: KAFKA-19312
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
The `onComplete` method in DelayedOperation is guaranteed to run only
once, through `forceComplete`, invoked either by `tryComplete` or when
operation is expired (`run` method). The invocation of  `tryComplete` is
done by attaining `lock` so no concurrent execution of  `tryComplete`
happens for same delayed operation. However, there can be  concurrent
execution of `tryComplete` and `onComplete` as the  `expiration` thread
can trigger a separte run of `onComplete` while  `tryComplete` is still
executing. This behaviour is not ideal as there  are parallel runs where
1 threads method execution is wasteful i.e.

**Before/After**
```java
// Before
        // We are utilizing lock so that onComplete doesn't do a dirty read for instance variables -
        // partitionsAcquired and localPartitionsAlreadyFetched, since these variables can get updated in a different tryComplete thread.
        lock.lock();
        try {
            if (remoteStorageFetchException.isPresent()) {
                completeErroneousRemoteShareFetchRequest();
            } else if (pendingRemoteFetchesOpt.isPresent()) {
                if (maybeRegisterCallbackPendingRemoteFetch()) {
                    log.trace("Registered remote storage fetch callback for group {}, member {}, "
                            + "topic partitions {}", shareFetch.groupId(), shareFetch.memberId(),
                        partitionsAcquired.keySet());
                    return;

// After
     * <p>
     * On operation timeout, onComplete is invoked, last try occurs to acquire partitions and read
     * from log, if acquired. The fetch will only happen from local log and not remote storage, on
     * operation expiration.
        if (remoteStorageFetchException.isPresent()) {
            completeErroneousRemoteShareFetchRequest();
        } else if (pendingRemoteFetchesOpt.isPresent()) {
            if (maybeRegisterCallbackPendingRemoteFetch()) {
                log.trace("Registered remote storage fetch callback for group {}, member {}, "
                        + "topic partitions {}", shareFetch.groupId(), shareFetch.memberId(),
                    partitionsAcquired.keySet());
                return;
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19312 avoiding concurrent execution of oncomplete and trycomplete (#19759).

**Test**
No test added.
