# Bug 1628: Fix long where int needed in commitlog segment

**Commit**: 65a46265df · **Ticket**: CASSANDRA-7199
**Subsystem**: db/commitlog (CommitLogSegment)
**Category**: Wrong constant / default value

---

## Root Cause

When writing a sync marker in the commit-log segment buffer, the code called `buffer.putLong(nextMarker + 4, 0)` to zero out the 4-byte CRC/size field immediately after the 4-byte sync marker. Using `putLong` wrote 8 bytes instead of 4, overwriting the first 4 bytes of the next record's data. The correct call is `buffer.putInt(nextMarker + 4, 0)`, matching the 4-byte field that was intended.

### Before / After

```java
// BEFORE
buffer.putInt(nextMarker, 0);
buffer.putLong(nextMarker + 4, 0);  // writes 8 bytes, stomping adjacent data
```

```java
// AFTER
buffer.putInt(nextMarker, 0);
buffer.putInt(nextMarker + 4, 0);   // writes exactly 4 bytes
```

---

## How to Spot

Any time a buffer-writing method (`putLong`, `putInt`, `putShort`) is chosen, verify that the method's width matches the field's declared size in the on-disk format. A mismatch silently corrupts adjacent bytes without throwing an exception if the buffer is large enough.

---

## Test

No test added.
