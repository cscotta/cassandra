# Bug 0036: CFK Not Cleaned Up After Accord Journal Replay

**Commit**: 4102ea13e5 · **Ticket**: CASSANDRA-20797
**Subsystem**: service/accord
**Category**: State not cleaned up

---

## Root Cause

After replaying the Accord journal on restart, `CommandsForKey` (CFK) could retain entries for unapplied transactions that were already known to be impossible to apply (e.g., they fell before the `redundantBefore` watermark). These stale entries were not pruned during the load path, causing CFK to hold incorrect state that could block or mislead waiting commands. Additionally, waiting commands were not forcibly notified during replay, so they could remain blocked indefinitely. A related sub-bug: `SaveStatus` and `executeAt` were not updated atomically together; expunge could see a state where `SaveStatus` had advanced but `executeAt` was still the old value, leading to corruption.

### Before / After

```java
// BEFORE: AccordCommandStore.loadCommandsForKey — raw load, no filtering
CommandsForKey loadCommandsForKey(RoutableKey key) {
    return CommandsForKeyAccessor.load(id, (TokenKey) key);
}
```

```java
// AFTER: apply redundantBefore filter on load
CommandsForKey loadCommandsForKey(RoutableKey key) {
    CommandsForKey cfk = CommandsForKeyAccessor.load(id, (TokenKey) key);
    if (cfk == null) return null;
    RedundantBefore.QuickBounds bounds = unsafeGetRedundantBefore().get(key);
    if (bounds == null) return cfk;
    return cfk.withRedundantBeforeAtLeast(bounds.gcBefore, false);
}
```

---

## How to Spot

When restoring durable state from a journal or SSTable on startup, always apply the current watermarks (redundant-before, GC-before, epoch bounds) to the loaded data before making it visible to the rest of the system. Stale entries that predate a known watermark are harmless in normal operation when the watermark is updated incrementally, but on replay they can be read back as if they were current.

---

## Test

`CommandChangeTest` was updated to include `Field.EXECUTE_AT` in the field-change coverage assertions, verifying that `SaveStatus` and `executeAt` are serialised together.
