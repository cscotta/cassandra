# Bug 1851: Fix json2sstable Handling of Range Tombstones

**Commit**: 159744f244 · **Ticket**: CASSANDRA-6316
**Subsystem**: tools/SSTableImport
**Category**: Wrong serialization / deserialization

---

## Root Cause

`SSTableImport` parsed JSON column entries with a simple two-branch condition: either the column was a deletion (hex bytes for the value) or a regular cell (value decoded via the column validator). Range tombstones were silently treated as regular cells, so their end-bound value was decoded through the cell value validator instead of the column comparator type. Because the end-bound of a range tombstone is a column name (not a cell value), this produced garbage bytes and dropped or corrupted the tombstone on import.

### Before / After

```java
// BEFORE
value = isDeleted() ? ByteBufferUtil.hexToBytes((String) fields.get(1))
                    : stringAsType((String) fields.get(1),
                          meta.getValueValidator(meta.getColumnDefinitionFromColumnName(name)));
```

```java
// AFTER
if (isDeleted()) {
    value = ByteBufferUtil.hexToBytes((String) fields.get(1));
} else if (isRangeTombstone()) {
    AbstractType<?> type = CompositeType.getInstance(
        Arrays.asList(new AbstractType<?>[]{meta.getColumnDefinitionComparator(0)}));
    value = type.fromString((String) fields.get(1));
} else {
    value = stringAsType((String) fields.get(1),
                meta.getValueValidator(meta.getColumnDefinitionFromColumnName(name)));
}
```

---

## How to Spot

When a binary format has more than two structural variants, a single `ternary` or `if/else` dispatch will silently fall through for any variant not explicitly handled. Always enumerate every variant and add an `else` that throws, so new cases are never silently misrouted.

---

## Test

No test added.
