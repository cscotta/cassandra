# Bug 0425: Double-Close of BaseIterator Counts Partition Cells Twice

**Commit**: 6baeb9247b · **Ticket**: CASSANDRA-16259
**Subsystem**: src/java/org/apache/cassandra/db/transform/BaseIterator.java
**Category**: State not cleaned up

---

## Root Cause

The cells-per-partition metric is updated inside the transformation applied to a `RowIterator` when that iterator is closed. `BaseIterator.close()` had no idempotency guard, so calling it a second time ran all `onClose` transformations again. Depending on which concrete `UnfilteredRowIterator` implementation was used, the caller could legally call `close()` twice (for example in try-with-resources combined with an explicit close), causing the per-partition cell counter to be incremented twice for the same partition and producing inflated metrics.

### Before / After

```java
// BEFORE
public final void close()
{
    Throwable fail = runOnClose(length);
    if (next instanceof AutoCloseable)
    {
        try { ((AutoCloseable) next).close(); }
        catch (Throwable t) { fail = merge(fail, t); }
    }
    try { input.close(); }
    catch (Throwable t) { fail = merge(fail, t); }
    maybeFail(fail);
}
```

```java
// AFTER
private boolean closed;

public final void close()
{
    if (closed)
        return;
    closed = true;
    // ... same body as before
}
```

---

## How to Spot

Any `close()` implementation that triggers side-effecting callbacks (metrics updates, listeners, transformations) rather than merely releasing resources should be idempotent. A reviewer should ask: "If this method is called twice, will the side effects fire twice?" For iterator wrappers that compose transformation chains, the answer is often yes unless an explicit closed-flag guards the body.

---

## Test

No new test was added in this commit; the fix was self-contained in `BaseIterator`.
