# Bug 0876: Null Dereference When Invalidating Keys With Empty Cache Map

**Commit**: c57d0d4fab · **Ticket**: CASSANDRA-12062
**Subsystem**: Storage / SSTableRewriter / key cache
**Category**: Missing null / bounds check

---

## Root Cause
`SSTableRewriter.maybeReopenEarly()` unconditionally built a list of keys to invalidate and passed a non-null `InvalidateKeys` runnable to `SSTableReader.runOnClose()`. `SSTableReader.runOnClose()` stored the runnable in a field that was later called as `andThen.run()` without a null check. When no keys were cached (`cachedKeys` was empty), the invalidation list was empty but the runnable was still created and registered, causing a null-pointer path further down the chain. Additionally, `SSTableReader.onCloseReopened()` called `andThen.run()` unconditionally, which NPE'd when `andThen` was null. The fix skips runnable creation entirely when `cachedKeys` is empty, and guards the `andThen.run()` call with a null check.

### Before / After
```java
// BEFORE — runnable always created, andThen.run() called unconditionally
final List<DecoratedKey> invalidateKeys = new ArrayList<>();
invalidateKeys.addAll(cachedKeys.keySet());
// ... populate newReader cache ...
cachedKeys = new HashMap<>();
for (SSTableReader sstable : transaction.originals())
{
    Runnable runOnClose = new InvalidateKeys(latest, invalidateKeys);
    latest.runOnClose(runOnClose);  // always set even when empty
    ...
}
// In SSTableReader:
andThen.run();  // NPE if andThen is null

// AFTER — skip creation when nothing to invalidate; guard the call
List<DecoratedKey> invalidateKeys = null;
if (!cachedKeys.isEmpty())
{
    invalidateKeys = new ArrayList<>(cachedKeys.size());
    // ... populate ...
}
cachedKeys.clear();
Runnable runOnClose = invalidateKeys != null ? new InvalidateKeys(latest, invalidateKeys) : null;
if (runOnClose != null)
    latest.runOnClose(runOnClose);
// In SSTableReader:
if (andThen != null)
    andThen.run();
```

---

## How to Spot
Whenever a `Runnable`/callback is stored in a field that was previously unconditionally set, verify that all call sites that invoke it guard against null, or that the field is always non-null by invariant. In lifecycle/compaction code, conditional callbacks (keyed to "if there are cached keys") should be null when the condition is false, not an empty-list no-op, so that callers can skip them cheaply and safely.

---

## Test
No dedicated test added; the fix is defensive null-guarding in the rewrite path.
