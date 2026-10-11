# Bug 0690: OldFormatDeserializer Position Calculation Off After Look-Ahead

**Commit**: 62092e45c8 · **Ticket**: CASSANDRA-13525
**Subsystem**: src/java/org/apache/cassandra/db/UnfilteredDeserializer.java
**Category**: Off-by-one

---

## Root Cause

`OldFormatDeserializer` groups legacy atoms into `Unfiltered` rows by reading ahead: the atom iterator calls `atoms.next()` to fetch the first atom of the *next* row before the current row has been returned to the caller. As a result, at the moment `readNext()` or `skipNext()` updates `lastConsumedPosition` by calling `currentPosition()`, the stream's file pointer is already past the first atom of the next row, not at the boundary of the row just consumed. This caused `bytesReadForUnconsumedData()` to return a negative or too-large value, which made the indexed block reader believe it had exhausted the block prematurely and stop reading data that was still within range.

### Before / After

```java
// BEFORE — position includes the look-ahead atom
public Unfiltered readNext() {
    Unfiltered toReturn = next;
    next = null;
    lastConsumedPosition = currentPosition(); // too far ahead
    return toReturn;
}
public void skipNext() {
    next = null;
    lastConsumedPosition = currentPosition(); // too far ahead
}
```

```java
// AFTER — subtract the size of the already-read next atom
private long bytesReadForNextAtom() {
    return iterator.atoms.next == null ? 0 : bytesReadForNextAtom;
}
public Unfiltered readNext() {
    Unfiltered toReturn = next;
    next = null;
    lastConsumedPosition = currentPosition() - bytesReadForNextAtom();
    return toReturn;
}
```

---

## How to Spot

In look-ahead iterators that track "how far have we consumed", always verify that the consumed-position update accounts for any bytes the iterator has read beyond the item being returned. If the iterator prefetches the next item to decide whether the current group is complete, the file cursor will be ahead of the logical end of the current item. A `bytesReadForUnconsumedData()` that can go negative is a reliable symptom.

---

## Test

A unit test for CASSANDRA-13236 was added to `LegacySSTableTest` using a real `ka` (2.1-format) SSTable with an index, verifying that range reads on legacy indexed partitions return complete results without premature termination.
