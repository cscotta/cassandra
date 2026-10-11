# Bug 2264: Unbounded Recursion in Leveled Compaction Scanner

**Commit**: b0342978a0 · **Ticket**: CASSANDRA-4587

## Detection Rule

When a `computeNext()` method calls itself recursively to advance past empty iterators, check whether the recursion depth is proportional to the number of items being iterated (stack overflow risk).

## Code Smell

A `computeNext()` implementation that calls `return computeNext()` to move to the next element rather than using a loop; each recursive call consumes a stack frame for every exhausted SSTable scanner.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: recursive-iterator-advance
- severity: crash
- subsystem: compaction

## Root Cause

The `LeveledScanner.computeNext()` method advanced to the next SSTable scanner by calling itself recursively. With many SSTables in a level, each exhausted scanner caused one recursive call, leading to a stack overflow when scanning a level with hundreds of SSTables. The fix replaced the recursion with an explicit `while(true)` loop.

## Example

```java
// BEFORE
if (currentScanner != null) {
    if (currentScanner.hasNext())
        return currentScanner.next();
    else {
        positionOffset += currentScanner.getLengthInBytes();
        currentScanner.close();
        currentScanner = null;
        return computeNext();  // recursive: one frame per SSTable
    }
}
if (!sstableIterator.hasNext())
    return endOfData();
currentScanner = sstableIterator.next().getDirectScanner(range);
return computeNext();  // recursive again
```

```java
// AFTER
while (true) {
    if (currentScanner.hasNext())
        return currentScanner.next();
    positionOffset += currentScanner.getLengthInBytes();
    currentScanner.close();
    if (!sstableIterator.hasNext())
        return endOfData();
    currentScanner = sstableIterator.next().getDirectScanner(range);
}
```
