# Bug 2183: Composite Index PREFIX_SIZE Off-by-One When Table Has Collections

**Commit**: 617a4ab6fd · **Ticket**: CASSANDRA-4909
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/CreateIndexStatement.java
**Category**: Off-by-one

---

## Root Cause

When creating a secondary index on a composite CQL3 table, `CreateIndexStatement` calculates `PREFIX_SIZE_OPTION` as `composite.types.size() - 1`. For tables that include collection columns, the composite type has an extra type appended (the collection-cell component), making the actual number of user-visible clustering components `composite.types.size() - 2` rather than `- 1`. Setting the wrong prefix size caused the composites index to include or exclude the wrong number of components, producing incorrect index reads or writes. Additionally, the fix adds an early rejection of index creation on collection columns themselves, which were not yet supported.

### Before / After

```java
// BEFORE
opts.put(CompositesIndex.PREFIX_SIZE_OPTION, String.valueOf(composite.types.size() - 1));
```

```java
// AFTER
if (cd.getValidator().isCollection())
    throw new InvalidRequestException("Indexes on collections are no yet supported");

opts.put(CompositesIndex.PREFIX_SIZE_OPTION,
         String.valueOf(composite.types.size() - (cfDef.hasCollections ? 2 : 1)));
```

---

## How to Spot

When computing a size or offset that depends on the structure of a composite type, check whether any additional "hidden" or structural types (e.g., collection markers, end-of-components bytes) are appended to the user-visible type list. Treating the raw `types.size()` as equivalent to the user-visible component count is a recurring source of off-by-one errors.

---

## Test

No test added.
