# Bug 0158: SAI Silently Drops Empty Byte Buffer Values for Types That Accept Them

**Commit**: 46acaf22e6 · **Ticket**: CASSANDRA-19461
**Subsystem**: `index/sai/disk/v1/SSTableIndexWriter`, `index/sai/memory/MemtableIndex`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Both the memtable SAI index path (`MemtableIndex.index()`) and the SSTable flush path (`SSTableIndexWriter.addRow()`) skipped indexing when the term's `ByteBuffer` had zero remaining bytes, regardless of the column type. For types such as `text`/`varchar` that explicitly allow the empty string as a valid value, this caused rows with an empty-string column value to be silently omitted from the index. Subsequent `WHERE col = ''` queries would return no results even though matching rows were present. The fix adds a per-type `allowsEmpty()` guard so that only types that treat an empty buffer as "no value" are skipped.

### Before / After

```java
// BEFORE — MemtableIndex.index(): unconditionally skips empty buffers
if (value == null || value.remaining() == 0)
    return 0;
```

```java
// AFTER — skips only when the type does not allow empty
if (value == null || (value.remaining() == 0 && !type.allowsEmpty()))
    return 0;
```

```java
// BEFORE — SSTableIndexWriter.addRow(): same unconditional skip
if (term.remaining() == 0) return;
```

```java
// AFTER
if (term.remaining() == 0 && !index.termType().indexType().allowsEmpty()) return;
```

---

## How to Spot

Any early-return guard of the form `if (buffer.remaining() == 0) skip` in an indexing or serialization path is a candidate for this bug whenever the type system has types that legitimately encode "the empty string" as a zero-length byte buffer. Always qualify such guards with a type-level predicate that distinguishes "no value" (null / tombstone) from "the valid empty value."

---

## Test

`EmptyStringLifecycleTest` inserts a row with `v = ''` and verifies that `SELECT * FROM t WHERE v = ''` returns it correctly from the memtable, after a flush, and after compaction.
