# Bug 0047: AccordJournalTable Delivers Journal Records Out of Order During Replay

**Commit**: 1c7e96b5d7 · **Ticket**: CASSANDRA-20718
**Subsystem**: service/accord/AccordJournalTable
**Category**: Logic error in condition

---

## Root Cause

`JournalAndTableRecordConsumer` tracked which segments had been read from the SSTable side (`visited` set) and used that to skip duplicates from the journal side, while `TableRecordConsumer` did the inverse. The bug was an inversion of responsibilities: `TableRecordConsumer` was marking segments as visited when reading from the SSTable (the correct behaviour for the SSTable side), but then `JournalAndTableRecordConsumer` was checking `tableRecordConsumer.visited()` to decide whether to skip journal records — meaning journal segments were skipped only when the SSTable had already seen them, not the other way around. The correct semantics are: journal records are primary and mark which segments are covered; the SSTable reader should then skip any segment that the journal has already delivered. The fix swaps the tracking: the journal consumer (`JournalAndTableRecordConsumer`) now builds the `visited` set, and the SSTable consumer (`TableRecordConsumer`) receives it read-only and filters accordingly.

### Before / After

```java
// BEFORE — TableRecordConsumer tracked visits; journal checked table's visited set (inverted)
class TableRecordConsumer {
    protected LongHashSet visited = null;
    void visit(long segment) { if (visited == null) visited = new LongHashSet(); visited.add(segment); }
    public void accept(...) { visit(segment); delegate.accept(...); } // marks AND forwards
}
class JournalAndTableRecordConsumer {
    private final TableRecordConsumer tableRecordConsumer;
    public void accept(...) {
        if (!tableRecordConsumer.visited(segment)) // wrong: skips journal if table saw it
            delegate.accept(...);
    }
}
```

```java
// AFTER — JournalAndTableRecordConsumer owns the visited set; TableRecordConsumer filters on it
class JournalAndTableRecordConsumer {
    private LongHashSet visited;
    void visit(long segment) { if (visited == null) visited = new LongHashSet(); visited.add(segment); }
    public void accept(...) { visit(segment); delegate.accept(...); }
    void readTable() { readAllFromTable(key, new TableRecordConsumer(visited, delegate)); }
}
class TableRecordConsumer {
    final LongHashSet visited; // read-only
    public void accept(...) { if (!visited(segment)) delegate.accept(...); }
}
```

---

## How to Spot

When two data sources (journal + SSTable) must be merged with deduplication, the ownership of the "already-seen" set must reflect which source is authoritative. If the wrong source populates the visited set, records from the authoritative source get dropped instead of duplicates from the secondary source. Review any dual-source merge pattern to confirm the visited/dedup set is populated by the primary source and used to filter the secondary source, not the reverse.

---

## Test

No dedicated test added for the ordering/deduplication logic; the fix relies on existing replay integration tests to catch regressions.
