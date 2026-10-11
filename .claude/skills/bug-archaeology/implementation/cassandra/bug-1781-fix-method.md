# Bug 1781: Fix CounterContext.ContextState.copyTo() method

**Commit**: b713721461 · **Ticket**: n/a
**Subsystem**: db/context (CounterContext)
**Category**: Logic error in condition

---

## Root Cause

In `CounterContext.ContextState.copyTo()`, when copying the header entry for a local counter shard, the code wrote to `context` (the source `ByteBuffer`) instead of `other.context` (the destination `ByteBuffer`). This one-character typo meant the destination was not updated for local shards, leaving stale or zeroed header data in the copy, while the source was silently corrupted.

### Before / After

```java
// BEFORE
else if (currentIsLocal)
    context.putShort(other.context.position() + other.headerOffset,
                     (short) other.getElementIndex());
```

```java
// AFTER
else if (currentIsLocal)
    other.context.putShort(other.context.position() + other.headerOffset,
                           (short) other.getElementIndex());
```

---

## How to Spot

In copy or serialization methods that involve two similar objects (`this` and `other`/`dest`), review every write operation and confirm the target buffer is the destination, not the source. A source write inside a `copyTo` method is almost always a bug.

---

## Test

No test added.
