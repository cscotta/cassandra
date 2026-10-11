# Bug 0069: Double Loading and Missing Journal-Appended Records During Accord Replay

**Commit**: 080d6c633d · **Ticket**: CASSANDRA-20244
**Subsystem**: `journal/Journal`, `service/accord/AccordJournal`, `service/accord/AccordJournalTable`
**Category**: State not cleaned up

---

## Root Cause

The Accord journal replay path had two interrelated problems. First, the old `AccordJournal.replay()` reconstructed each command by reading raw diffs from the journal table and manually deserializing them, then independently loading the command again via `loader.load(command)` — effectively reading each command twice. Second, the `StaticSegmentIterator` used a `PriorityQueue` of per-segment readers, but records written to the *active* (non-static) journal segment since the last flush were invisible to the iterator, so commands whose most recent changes had not yet been compacted to a static segment were silently lost on replay.

The fix replaced the hand-rolled iterator with a `MergeIterator`-based `StaticSegmentKeyIterator` that tracks which segments reference each key, and delegated actual command reconstruction to the store's `Loader.load(TxnId)`, which correctly reads from both static segments and the live journal segment. The `closeCurrentSegmentForTestingIfNonEmpty` call was also moved into a test-only override so production code no longer unconditionally closes the active segment before replay.

### Before / After

```java
// BEFORE – double-loads command; misses active segment records
journal.closeCurrentSegmentForTestingIfNonEmpty();
try (AccordJournalTable.KeyOrderIterator<JournalKey> iter = journalTable.readAll())
{
    while ((key = iter.key()) != null)
    {
        iter.readAllForKey(key, (seg, pos, k, buf, ver) -> { builder.deserializeNext(...); });
        Command command = builder.construct(...);
        async(loader::load, command).get();    // second load
    }
}
```

```java
// AFTER – single load via Loader; includes live-segment records
try (CloseableIterator<Journal.KeyRefs<JournalKey>> iter = journalTable.keyIterator())
{
    while (iter.hasNext())
    {
        Journal.KeyRefs<JournalKey> ref = iter.next();
        if (ref.key().type != JournalKey.Type.COMMAND_DIFF) continue;
        CommandStore commandStore = commandStores.forId(ref.key().commandStoreId);
        AsyncChains.getUnchecked(commandStore.loader().load(ref.key().id)...);
    }
}
```

---

## How to Spot

Replay or recovery loops that both deserialize records *and* then pass the partially-reconstructed object to a `load()` API perform duplicate work and can diverge if the reconstruction logic is not identical. Always prefer delegating reconstruction to the authoritative load path rather than inline deserialization in the replay loop. Also verify that segment iterators used during replay cover the *active* write segment, not only sealed/static segments.

---

## Test

`AccordJournalBurnTest.testOne` un-ignored with the `closeCurrentSegmentForTestingIfNonEmpty` call moved to a test-only `replay` override, exercising the recovery path including the active segment.
