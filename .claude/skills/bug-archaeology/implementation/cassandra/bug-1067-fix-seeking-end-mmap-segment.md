# Bug 1067: Seeking to End of Mmap Segment Throws IOException

**Commit**: f87e34ed3c · **Ticket**: CASSANDRA-10478
**Subsystem**: src/java/org/apache/cassandra/io/util/MappedFileDataInput.java
**Category**: Off-by-one

---

## Root Cause

`MappedFileDataInput.contains()` used a strict less-than comparison (`inSegmentPos < buffer.capacity()`) to decide whether a position was valid. This meant a seek to exactly `segmentOffset + buffer.capacity()` — i.e. the position immediately past the last byte, which is the canonical EOF position — was rejected as out-of-bounds. The `seek()` method delegated to `contains()`, so seeking to the end-of-segment EOF position always threw an `IOException`, breaking any code that tried to position a reader at the exact end of a mapped segment.

### Before / After

```java
// BEFORE — strict less-than rejects the EOF position
public boolean contains(long pos)
{
    long inSegmentPos = pos - segmentOffset;
    return inSegmentPos >= 0 && inSegmentPos < buffer.capacity();
}
```

```java
// AFTER — seek() allows position == capacity (EOF); contains() still uses strict less-than for membership tests
public void seek(long pos) throws IOException
{
    long inSegmentPos = pos - segmentOffset;
    if (inSegmentPos < 0 || inSegmentPos > buffer.capacity())   // <= instead of <
        throw new IOException(...);
    seekInternal((int) inSegmentPos);
}
```

---

## How to Spot

In any bounded-buffer or segment abstraction, verify that "valid position" and "valid seek target" are treated separately. A position is valid for reading if it is strictly less than the capacity; a position is valid for seeking if it is less than or equal to the capacity (EOF is a legitimate seek target). Any method that uses the same bound for both read-validity and seek-validity is a candidate for this off-by-one error.

---

## Test

New `MappedFileDataInputTest` added, explicitly testing that seeking to `segmentOffset + capacity` (EOF) succeeds and that `isEOF()` returns true, while seeking one byte past the end throws `IOException`.
