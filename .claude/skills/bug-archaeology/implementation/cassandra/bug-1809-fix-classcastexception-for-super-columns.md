# Bug 1809: Fix ClassCastException for super columns

**Commit**: 694988015f · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/thrift/CassandraServer.java
**Category**: Logic error in condition

---

## Root Cause

When building a `NamesQueryFilter` for a super-column read in the Thrift layer, the code used the table's composite `CellNameType` directly as the comparator for a `TreeSet<ByteBuffer>`. Because the composite type expects multi-component cell names rather than raw `ByteBuffer` values, any comparison triggered a `ClassCastException`. The fix wraps the correct subtype component in a `SimpleDenseCellNameType`, constructs the set with properly typed `CellName` objects, and converts the raw bytes via `cellFromByteBuffer`.

### Before / After

```java
// BEFORE
CellNameType type = metadata.comparator;
SortedSet names = new TreeSet<ByteBuffer>(column_path.column == null ? type.subtype(0) : type.subtype(1));
names.add(column_path.column == null ? column_path.super_column : column_path.column);
filter = SuperColumns.fromSCNamesFilter(type, ..., new NamesQueryFilter(names));
```

```java
// AFTER
CellNameType columnType = new SimpleDenseCellNameType(metadata.comparator.subtype(column_path.column == null ? 0 : 1));
SortedSet<CellName> names = new TreeSet<CellName>(columnType);
names.add(columnType.cellFromByteBuffer(column_path.column == null ? column_path.super_column : column_path.column));
filter = SuperColumns.fromSCNamesFilter(metadata.comparator, ..., new NamesQueryFilter(names));
```

---

## How to Spot

Raw / unchecked `SortedSet` or `TreeSet` construction with a comparator that is not type-compatible with the elements added is a red flag. Always check that the element type stored in the collection matches the type parameter the comparator expects.

---

## Test

No test added.
