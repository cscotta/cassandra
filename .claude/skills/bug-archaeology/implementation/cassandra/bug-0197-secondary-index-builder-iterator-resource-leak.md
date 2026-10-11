# Bug 0197: SecondaryIndexBuilder Iterator Not Closed on Success Path

**Commit**: 016d91a7d7 · **Ticket**: CASSANDRA-18361
**Subsystem**: index/SecondaryIndexManager, index/internal/CollatedViewIndexBuilder
**Category**: Resource leak

---

## Root Cause

`CollatedViewIndexBuilder.build()` wrapped its iteration in a `try/finally` that closed the underlying `ReducingKeyIterator` unconditionally, but the `finally` only ran when `build()` itself returned — it did not account for the fact that the builder was submitted to `CompactionManager` asynchronously. In `SecondaryIndexManager`, the async `FutureCallback` only handled cleanup of index state on failure or success flags, but never closed the builder. The result was that the `ReducingKeyIterator` (which holds open SSTable file handles) was leaked whenever the index build succeeded and the async callback fired: `build()` exited the `try/finally` block and closed the iterator, but the `finally` was inside `build()` so it depended on `build()` being the canonical lifetime owner, which it was not once the builder became `AutoCloseable`.

The fix made `CollatedViewIndexBuilder` implement `AutoCloseable` and moved the `iter.close()` call into the new `close()` method. Both the success and failure callbacks in `SecondaryIndexManager` were updated to call `builder.close()`, and `CassandraIndex`'s direct usage of the builder gained an explicit `Futures.addCallback` that calls `builder.close()` in both outcomes.

### Before / After

```java
// BEFORE – iter closed inside build(), but close is only guaranteed if build() is the owner
public void build() {
    try {
        while (iter.hasNext()) { ... }
    } finally {
        iter.close();  // not reached if close happens through async path
    }
}
```

```java
// AFTER – build() has no finally; close is explicit via AutoCloseable
public void build() {
    while (iter.hasNext()) { ... }
}

@Override
public void close() {
    iter.close();
}
```

---

## How to Spot

When a resource owner is submitted to an executor and its lifecycle is managed through callbacks, the resource must be closed in the callback, not inside the method that runs on the executor. Embedding `close()` in a `try/finally` inside the task body is only safe when the caller holds no other reference to the resource and the task is the sole owner of the lifetime. Any time a `SecondaryIndexBuilder` (or similar task) holds file handles and is submitted asynchronously, review that every outcome path (success, failure, cancellation) triggers `close()`.

---

## Test

No new test added; the fix is structural — `CollatedViewIndexBuilder` now implements `AutoCloseable` and close is explicitly invoked from all callback paths.
