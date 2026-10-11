# Bug 2847: SSTableImport inverts addColumn/addTombstone for deleted columns

**Commit**: 22666f1c34 · **Ticket**: CASSANDRA-1753

## Detection Rule
When importing JSON-exported SSTables, verify that `col.isDeleted == true` routes to `addTombstone(...)` and `col.isDeleted == false` routes to `addColumn(...)`; an accidental swap causes live data to be imported as tombstones and tombstones as live data.

## Code Smell
```java
if (col.isDeleted) {
    cfamily.addColumn(path, ...);  // wrong: should be addTombstone
} else {
    cfamily.addTombstone(path, ...); // wrong: should be addColumn
}
```
The if/else branches are reversed, so every imported deleted column becomes a live column and vice versa.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: inverted-branch-condition
- severity: data-loss
- subsystem: tools-clients

## Root Cause
A regression swapped the `addColumn` and `addTombstone` calls inside the `isDeleted` check in both standard and super-column import paths. Any data imported via `sstableimport` would silently resurrect deleted rows as live data and turn live data into tombstones.

## Example
```java
// BEFORE
if (col.isDeleted) {
    cfamily.addColumn(path, ByteBuffer.wrap(hexToBytes(col.value)), col.timestamp);
} else {
    cfamily.addTombstone(path, ByteBuffer.wrap(hexToBytes(col.value)), col.timestamp);
}

// AFTER
if (col.isDeleted) {
    cfamily.addTombstone(path, ByteBuffer.wrap(hexToBytes(col.value)), col.timestamp);
} else {
    cfamily.addColumn(path, ByteBuffer.wrap(hexToBytes(col.value)), col.timestamp);
}
```
