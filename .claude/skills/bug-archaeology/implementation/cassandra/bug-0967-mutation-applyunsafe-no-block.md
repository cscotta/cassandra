# Bug 0967: Mutation.applyUnsafe Does Not Block

**Commit**: c4d83f1ba3 · **Ticket**: CASSANDRA-10779
**Subsystem**: Storage engine / mutation application
**Category**: Logic error in condition

---

## Root Cause
`Mutation.applyUnsafe` was supposed to apply a mutation without durable writes (skipping the commit log) but still wait for the operation to complete. Instead it called `Keyspace.open(keyspaceName).apply(this, false)` directly — the raw fire-and-forget `apply` — which returns a `CompletableFuture` that was silently discarded, meaning `applyUnsafe` returned immediately without blocking on the keyspace write. Meanwhile, the `apply(boolean durableWrites)` overload had the same fire-and-forget problem. The fix refactors all three overloads so they all funnel through a private `applyFuture(boolean durableWrites)` and then call `Uninterruptibles.getUninterruptibly(...)` to block.

### Before / After
```java
// BEFORE
public void apply(boolean durableWrites)
{
    Keyspace.open(keyspaceName).apply(this, durableWrites);  // returns Future, discarded
}

public void applyUnsafe()
{
    Keyspace.open(keyspaceName).apply(this, false);           // also discarded
}

// AFTER
private CompletableFuture<?> applyFuture(boolean durableWrites)
{
    return Keyspace.open(keyspaceName).apply(this, durableWrites);
}

public void apply(boolean durableWrites)
{
    try { Uninterruptibles.getUninterruptibly(applyFuture(durableWrites)); }
    catch (ExecutionException e) { throw new RuntimeException(e); }
}

public void applyUnsafe()
{
    apply(false);   // now correctly blocks, skips commit log
}
```

---

## How to Spot
Methods that delegate to an async keyspace operation and ignore the returned `Future` will silently fail to block. During review, search for calls to `Keyspace.apply` that are not followed by a `get()` or `Uninterruptibles.getUninterruptibly()` call on the returned future. Any `applyUnsafe` / "unsafe" write utility should be scrutinized to confirm "unsafe" means "no durable write" rather than "no wait".

---

## Test
No test added.
