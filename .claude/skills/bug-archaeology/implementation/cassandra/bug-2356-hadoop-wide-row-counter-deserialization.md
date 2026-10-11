# Bug 2356: Hadoop wide-row reader deserialises counter columns as simple columns, producing corrupt data

**Commit**: edf9e0e169 · **Ticket**: CASSANDRA-4181

## Detection Rule

When iterating `ColumnOrSuperColumn` results from Thrift, check that counter columns are dispatched through `unthriftify(cosc)` (which inspects the union type) rather than `unthriftifySimple(cosc.column)` (which unconditionally reads the simple column field and crashes or produces garbage for counter columns).

## Code Smell

`ImmutableSortedMap.of(cosc.column.name, unthriftifySimple(cosc.column))` directly accesses `cosc.column` — if the column-or-super-column is actually a counter column, `cosc.column` is `null` and this throws a NPE; even when non-null, `unthriftifySimple` does not produce a valid counter `IColumn`.

## Tags

- scope: cassandra
- category: wrong-serialization
- code-pattern: wrong-deserialization-method
- severity: crash
- subsystem: tools-clients

## Root Cause

`ColumnFamilyRecordReader.WideRowIterator` used `unthriftifySimple(cosc.column)` to convert each paged result column into an `IColumn`. For counter column families, `cosc.column` is `null` and `cosc.counter_column` holds the value. The generic `unthriftify(cosc)` method dispatches correctly between simple, counter, super, and counter-super variants. The fix replaces the direct field access and simple-deserialise call with `unthriftify(cosc)` and uses `column.name()` to get the key.

## Example

```java
// BEFORE
ColumnOrSuperColumn cosc = columns.next();
ImmutableSortedMap<ByteBuffer, IColumn> map =
        ImmutableSortedMap.of(cosc.column.name, unthriftifySimple(cosc.column));
```

```java
// AFTER
ColumnOrSuperColumn cosc = columns.next();
IColumn column = unthriftify(cosc);   // handles counter / super / etc.
ImmutableSortedMap<ByteBuffer, IColumn> map =
        ImmutableSortedMap.of(column.name(), column);
```
