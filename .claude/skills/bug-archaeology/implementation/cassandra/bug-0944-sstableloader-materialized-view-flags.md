# Bug 0944: SSTableLoader Fails with Materialized View

**Commit**: 19bccb4eae · **Ticket**: CASSANDRA-11275
**Subsystem**: SSTableLoader / Schema
**Category**: Wrong constant / default value

---

## Root Cause
`NativeSSTableLoaderClient` unconditionally read the `flags` column from `system_schema.tables` to determine whether a table was compound (`isCompound`). Materialized views do not store a `flags` set in that column, so the read returned an empty set. With `isCompound` false, the loader constructed the wrong `CFMetaData` for the view, causing it to fail loading SSTable files belonging to that view.

The fix treats views as always compound and always without the super/counter/dense flags by short-circuiting the flags lookup when `isView` is `true`.

### Before / After
```java
// BEFORE
Set<CFMetaData.Flag> flags = CFMetaData.flagsFromStrings(row.getSet("flags", String.class));
boolean isCompound = flags.contains(CFMetaData.Flag.COMPOUND);

// AFTER
Set<CFMetaData.Flag> flags = isView ? Collections.emptySet()
                                     : CFMetaData.flagsFromStrings(row.getSet("flags", String.class));
boolean isCompound = isView || flags.contains(CFMetaData.Flag.COMPOUND);
```

---

## How to Spot
When a code path reads table-descriptor columns from the schema tables, check whether all schema object types (base tables, materialized views, secondary-index backing tables) are present in the result set and whether the columns being read have the same semantics for every type. A missing `isView` guard before reading a column that only exists meaningfully for regular tables is a reliable signal of this class of bug.

---

## Test
No test added.
