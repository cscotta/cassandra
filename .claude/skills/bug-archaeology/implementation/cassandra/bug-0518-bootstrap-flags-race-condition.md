# Bug 0518: Race Condition When Setting Bootstrap Completion Flags

**Commit**: 301168b603 · **Ticket**: CASSANDRA-14878
**Subsystem**: `service/StorageService.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

Bootstrap completion was signalled via a `Futures.addCallback` on the bootstrap stream future, which means `bootstrapFinished()` was called from a Guava callback thread — a different thread from the one waiting on `bootstrapStream.get()`. This created a race: the main bootstrap thread could observe the future as done (i.e., `get()` returned) and proceed with post-bootstrap work before `bootstrapFinished()` had executed on the callback thread, leaving the node in an inconsistent state where it was serving traffic without its bootstrap flags properly set. The fix removes the async callback entirely and calls `bootstrapFinished()` synchronously on the same thread immediately after `bootstrapStream.get()` returns successfully.

### Before / After

```java
// BEFORE – async callback races with get() return
Futures.addCallback(bootstrapStream, new FutureCallback<StreamState>() {
    public void onSuccess(StreamState streamState) {
        bootstrapFinished();
        logger.info("Bootstrap completed! for the tokens {}", tokens);
    }
    public void onFailure(Throwable e) {
        logger.warn("Error during bootstrap.", e);
    }
});
try {
    bootstrapStream.get();
    return true;
}
```

```java
// AFTER – synchronous, ordered execution
try {
    bootstrapStream.get();
    bootstrapFinished();
    logger.info("Bootstrap completed for tokens {}", tokens);
    return true;
}
```

---

## How to Spot

A pattern of registering a callback on a future AND also blocking on `future.get()` in the same code path is a strong signal of a race: the get() caller will proceed as soon as the future completes, potentially before the callback has run. Any state mutations that must happen-before subsequent observable actions should be placed after `get()`, not in a callback. Reviewers should ask: "if `get()` returns first and the callback hasn't fired yet, is the system in a valid state?"

---

## Test

No test added.
