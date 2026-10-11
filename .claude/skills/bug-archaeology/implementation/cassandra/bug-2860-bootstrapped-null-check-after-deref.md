# Bug 2860: isBootstrapped NPE when bootstrap column family is null

**Commit**: 0e97a3869f · **Ticket**: n/a

## Detection Rule
When reading a column from a `ColumnFamily` result that may be null (e.g., a freshly initialized node with no system table row), check `cf != null` before calling `cf.getColumn(...)`.

## Code Smell
`IColumn c = cf.getColumn(BOOTSTRAP); return cf != null && c.value().array()[...]` — the null check on `cf` comes after `cf.getColumn()` is already called. On a first-run node the column family doesn't exist yet, so `cf` is null and `cf.getColumn(BOOTSTRAP)` throws NPE before the guard is evaluated.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: null-check-after-dereference
- severity: crash
- subsystem: bootstrap-startup

## Root Cause
`SystemTable.isBootstrapped()` queried the system table for the bootstrap marker column. On a first boot the column family does not exist and `getColumnFamily()` returns null. The null check `cf != null` was placed in the return expression after `cf.getColumn(BOOTSTRAP)` had already been evaluated, causing NPE. The fix moves `if (cf == null) return false` before any field access, and also replaces unsafe `array()[position+arrayOffset]` with `ByteBuffer.get(0)`.

## Example
```java
// BEFORE
ColumnFamily cf = table.getColumnFamilyStore(STATUS_CF).getColumnFamily(filter);
IColumn c = cf.getColumn(BOOTSTRAP);
return cf != null && c.value().array()[c.value().position()+c.value().arrayOffset()] == 1;

// AFTER
ColumnFamily cf = table.getColumnFamilyStore(STATUS_CF).getColumnFamily(filter);
if (cf == null)
    return false;
IColumn c = cf.getColumn(BOOTSTRAP);
return c.value().get(0) == 1;
```
