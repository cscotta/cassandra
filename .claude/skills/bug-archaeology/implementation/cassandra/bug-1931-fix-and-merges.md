# Bug 1931: Bad Merge Corrupts UntypedResultSet.Row Field Initialization

**Commit**: 349d5dbead · **Ticket**: CASSANDRA-6000, CASSANDRA-5990
**Subsystem**: src/java/org/apache/cassandra/cql3/UntypedResultSet.java, src/java/org/apache/cassandra/config/CFMetaData.java, src/java/org/apache/cassandra/hadoop/pig/CassandraStorage.java
**Category**: State not cleaned up

---

## Root Cause

A bad merge left `UntypedResultSet.Row` with a `columns` field declared as `final List<ColumnSpecification>` but never initialized inline, so it defaulted to `null`. When the `Row(List<ColumnSpecification>, List<ByteBuffer>)` constructor ran `this.columns = names`, it assigned the parameter reference — but the field had no backing list for `addAll`. The fix initializes `columns` at declaration (`= new ArrayList<>()`) and changes the constructor to use `this.columns.addAll(names)` so the list is always non-null and consistently populated. Two other merge artifacts were also fixed: a single-line catch block reformatted for clarity, and incorrect import consolidation in `CassandraStorage`.

### Before / After

```java
// BEFORE
final List<ColumnSpecification> columns;  // null until constructor

public Row(List<ColumnSpecification> names, List<ByteBuffer> columns)
{
    this.columns = names;  // assigns but field could be null elsewhere
    ...
}
```

```java
// AFTER
private final List<ColumnSpecification> columns = new ArrayList<>();

public Row(List<ColumnSpecification> names, List<ByteBuffer> columns)
{
    this.columns.addAll(names);  // safe: field always initialized
    ...
}
```

---

## How to Spot

After a merge, review fields in data-holder classes to ensure final fields are initialized at declaration or in every constructor. A final field assigned in one constructor but not initialized at declaration can appear non-null while actually being null in paths that do not use that constructor.

---

## Test

No test added.
