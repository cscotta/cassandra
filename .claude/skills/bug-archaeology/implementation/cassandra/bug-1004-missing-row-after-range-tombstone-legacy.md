# Bug 1004: Missing Row Data After Range Tombstone in Legacy Data

**Commit**: 71bac92cf1 · **Ticket**: CASSANDRA-10822
**Subsystem**: Storage engine / legacy SSTable deserialization
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
The legacy (pre-3.0) `UnfilteredDeserializer` iterated atoms from a 2.x SSTable and used `TombstoneTracker.isShadowed()` to skip atoms whose timestamps fell inside an open range tombstone.
The shadowing check was placed inside the wrong loop branch for the modern-format path, causing the loop to spin on `next == null` indefinitely rather than consuming the shadowed atom and moving forward.
For the legacy-format path, the check was missing entirely: regular row atoms that followed a range tombstone were emitted without testing whether they were shadowed, so rows whose timestamps were older than the enclosing range tombstone appeared in query results even though they should have been suppressed.
A secondary fix handled the case where the legacy SSTable had no serialization header: `CompactionManager.createWriter` would pass a `null` header to `SSTableWriter`, leading to a `NullPointerException` during upgrade compaction of old-format files.

### Before / After
```java
// BEFORE — legacy path: atom emitted unconditionally, no shadowing check:
LegacyLayout.LegacyAtom atom = atoms.next();
next = isRow(atom) ? readRow(atom) : tombstoneTracker.openNew(atom.asRangeTombstone());

// AFTER — check before emitting:
LegacyLayout.LegacyAtom atom = atoms.next();
if (!tombstoneTracker.isShadowed(atom))
    next = isRow(atom) ? readRow(atom) : tombstoneTracker.openNew(atom.asRangeTombstone());

// BEFORE — CompactionManager: null header passed directly to writer:
return SSTableWriter.create(..., sstable.header, txn);

// AFTER — derive header when absent:
SerializationHeader header = sstable.header;
if (header == null)
    header = SerializationHeader.make(sstable.metadata, Collections.singleton(sstable));
return SSTableWriter.create(..., header, txn);
```

---

## How to Spot
Whenever a deserializer loop is responsible for both emitting and skipping atoms, every branch that can produce an atom must run the skip predicate. A `while (next == null)` loop that calls `openNew` or `readRow` without first testing `isShadowed` is a dead giveaway: the tombstone tracker's state advances with each atom but the emitted result never reflects the accumulated tombstone coverage. Code review should verify that the shadowing guard is applied symmetrically to all atom types in every branch.

---

## Test
No test added.
