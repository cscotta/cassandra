# Bug 2832: CLI supercolumn detection checks wrong field

**Commit**: 2f5f0c2c12 · **Ticket**: CASSANDRA-1899

## Detection Rule
When checking whether a column family is a super-column type, check `CfDef.column_type` (the column family type field), not `CfDef.comparator_type` (the comparator class name).

## Code Smell
`cfDef.comparator_type.equals("Super")` will never be true because `comparator_type` holds a comparator class name (e.g., `"BytesType"`), not the CF type string. The correct field is `cfDef.column_type`, which holds `"Super"` or `"Standard"`.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: wrong-field-for-type-check
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`CfDef` has two separate string fields: `column_type` (which is `"Super"` for super-column families) and `comparator_type` (which names the comparator class). The `isSuper` check used `comparator_type`, so super-column `get` commands in the CLI always treated the CF as a standard family, resulting in wrong key/column parsing.

## Example
```java
// BEFORE
boolean isSuper = cfDef.comparator_type.equals("Super");

// AFTER
boolean isSuper = cfDef.column_type.equals("Super");
```
