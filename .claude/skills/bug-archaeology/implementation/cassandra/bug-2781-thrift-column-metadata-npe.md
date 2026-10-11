# Bug 2781: NPE When Thrift client Omits column_metadata Field in CF Definition

**Commit**: 55c153f2f6 · **Ticket**: CASSANDRA-2096

## Detection Rule
When iterating `def.getColumn_metadata()` during Thrift-to-Avro CF definition conversion, first check `def.isSetColumn_metadata()` to avoid a NullPointerException on a null list.

## Code Smell
`for (org.apache.cassandra.thrift.ColumnDef cdef : def.getColumn_metadata())` calls `getColumn_metadata()` unconditionally; if the Thrift client did not set this optional field the list is null and the for-each loop throws NPE.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: iterate-optional-thrift-field-without-isSet
- severity: crash
- subsystem: schema-metadata

## Root Cause
`CFMetaData.convertToAvro` iterated `def.getColumn_metadata()` without checking whether the caller populated the optional field. Thrift-generated objects return `null` for unset optional fields unless accessed via the `isSet*` guard. Passing a `CfDef` without `column_metadata` caused an NPE. The fix wraps the loop in `if (def.isSetColumn_metadata())`.

## Example
```java
// BEFORE
for (org.apache.cassandra.thrift.ColumnDef cdef : def.getColumn_metadata()) {
    // NPE if column_metadata not set
}
```
```java
// AFTER
if (def.isSetColumn_metadata()) {
    for (org.apache.cassandra.thrift.ColumnDef cdef : def.getColumn_metadata()) {
        // safe
    }
}
```
