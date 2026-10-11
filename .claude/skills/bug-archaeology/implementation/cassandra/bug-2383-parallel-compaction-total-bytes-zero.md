# Bug 2383: ParallelCompactionIterable doesn't initialise totalBytes, reporting zero progress

**Commit**: 3931ee709d · **Ticket**: CASSANDRA-3758

## Detection Rule

When a subclass of a compaction iterable overrides the constructor to set `totalBytes = 0` locally but the parent class also initialises `totalBytes`, check whether the parent initialisation is bypassed — if the subclass resets `totalBytes` to 0 after the parent sets it, JMX compaction progress will always read 0%.

## Code Smell

`ParallelCompactionIterable` (and `CompactionIterable`) each compute and set `totalBytes` in their own constructors by summing scanner file lengths, duplicating logic with the abstract base class; if the base-class version is called *after* the subclass assignment, the subclass value is overwritten.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: subclass-duplicates-parent-init
- severity: performance
- subsystem: compaction

## Root Cause

`AbstractCompactionIterable` had no knowledge of the scanners list and did not initialise `totalBytes`. Each concrete subclass (`CompactionIterable`, `ParallelCompactionIterable`) initialised `totalBytes` and `bytesRead` itself. `ParallelCompactionIterable` called `super(controller, type)` first, then set `this.scanners = scanners`, so `totalBytes` was correctly computed only in the subclass — but the duplication meant any future refactor could easily break one. The fix moves the scanner list and `totalBytes` initialisation to the base constructor, passing `scanners` via `super(controller, type, scanners)`, and removes the duplicate logic from subclasses.

## Example

```java
// BEFORE (CompactionIterable)
protected CompactionIterable(..., List<SSTableScanner> scanners, ...) {
    super(controller, type);
    this.scanners = scanners;
    totalBytes = bytesRead = 0;
    for (SSTableScanner scanner : scanners)
        totalBytes += scanner.getFileLength(); // duplicated in each subclass
}
```

```java
// AFTER (AbstractCompactionIterable)
public AbstractCompactionIterable(CompactionController controller,
                                   OperationType type, List<SSTableScanner> scanners) {
    this.scanners = scanners;
    long bytes = 0;
    for (SSTableScanner scanner : scanners)
        bytes += scanner.getFileLength();
    this.totalBytes = bytes; // one place, shared by all subclasses
}
```
