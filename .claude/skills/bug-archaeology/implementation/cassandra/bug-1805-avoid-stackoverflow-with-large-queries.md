# Bug 1805: StackOverflow in CQL SELECT Iterator for Large IN Clauses

**Commit**: 34058783f5 · **Ticket**: CASSANDRA-6567
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Logic error in condition

---

## Root Cause

`SelectStatement` used a recursive Guava `AbstractIterator` to walk over the columns requested by an `IN` clause: when a requested column was not present in the column family, `computeNext()` called itself recursively. With a large `IN` list where many requested columns are absent, the recursion depth equals the number of missing columns, quickly overflowing the JVM stack. The fix replaces the recursive call with an iterative `while` loop, eliminating any stack growth regardless of how many consecutive columns are missing.

### Before / After

```java
// BEFORE – recursive: each missing column adds a stack frame
public IColumn computeNext() {
    if (!iter.hasNext())
        return endOfData();
    IColumn column = cf.getColumn(iter.next());
    return column == null ? computeNext() : column;  // unbounded recursion
}
```

```java
// AFTER – iterative
public IColumn computeNext() {
    while (iter.hasNext()) {
        IColumn column = cf.getColumn(iter.next());
        if (column != null)
            return column;
    }
    return endOfData();
}
```

---

## How to Spot

Any `AbstractIterator.computeNext()` that calls itself recursively on a "skip" condition is a latent `StackOverflowError`. The safe pattern for skipping is always an iterative loop, not recursion.

---

## Test

No test added.
