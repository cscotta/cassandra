# Bug 1301: Prevent AssertionError from SizeEstimatesRecorder

**Commit**: bd4842410e · **Ticket**: CASSANDRA-9034
**Subsystem**: db/SizeEstimatesRecorder.java
**Category**: Missing null / bounds check

---

## Root Cause

`SizeEstimatesRecorder.run()` is scheduled as a periodic background task that records partition size estimates for each token range owned by the local node. It called `StorageService` APIs that require the node to have fully joined the ring, but the scheduler could fire the task before the node had completed startup. This triggered an `AssertionError` (or similar invariant violation) inside the ring membership code because the local token ranges are undefined until the node has joined. The fix adds an early return when `StorageService.instance.isStarting()` is true, so the task simply skips the recording pass and waits for the next scheduled interval.

### Before / After

```java
// BEFORE — proceeds immediately, no startup guard
public void run()
{
    logger.debug("Recording size estimates");
    // find primary token ranges for the local node.
```

```java
// AFTER — guards against pre-join execution
public void run()
{
    if (StorageService.instance.isStarting())
    {
        logger.debug("Node has not yet joined; not recording size estimates");
        return;
    }
    logger.debug("Recording size estimates");
```

---

## How to Spot

Background tasks that are scheduled at startup and depend on node state (ring membership, schema, token ownership) must guard against firing before that state is initialized. Any periodic `Runnable` that calls into `StorageService`, `Keyspace`, or token-range APIs without first checking node lifecycle state is a candidate for this class of bug.

---

## Test

No test added.
