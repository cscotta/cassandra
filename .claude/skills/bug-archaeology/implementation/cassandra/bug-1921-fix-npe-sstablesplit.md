# Bug 1921: Null compacting set passed to getFullyExpiredSSTables causes NPE during sstablesplit

**Commit**: 56b5c6f302 · **Ticket**: CASSANDRA-6027
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionController.java
**Category**: Missing null / bounds check

---

## Root Cause

`CompactionController.getFullyExpiredSSTables` iterates over the `compacting` set to identify fully-expired SSTables, but callers such as the sstablesplit tool can pass `null` for that parameter when there is no active compaction context. The method immediately used the reference without a null guard, causing a `NullPointerException` before any useful work could be done.

### Before / After

```java
// BEFORE
public static Set<SSTableReader> getFullyExpiredSSTables(..., Set<SSTableReader> compacting, ...) {
    logger.debug("Checking droppable sstables in {}", cfStore);
    List<SSTableReader> candidates = new ArrayList<SSTableReader>();
    // NPE here if compacting is null
```

```java
// AFTER
public static Set<SSTableReader> getFullyExpiredSSTables(..., Set<SSTableReader> compacting, ...) {
    logger.debug("Checking droppable sstables in {}", cfStore);

    if (compacting == null)
        return Collections.<SSTableReader>emptySet();

    List<SSTableReader> candidates = new ArrayList<SSTableReader>();
```

---

## How to Spot

When a static utility method accepts a collection parameter that flows from multiple call sites, check whether all callers can supply a non-null value. Tool-mode callers (like sstablesplit) often construct a minimal context and may legitimately pass `null` for sets representing "no active set."

---

## Test

No test added.
