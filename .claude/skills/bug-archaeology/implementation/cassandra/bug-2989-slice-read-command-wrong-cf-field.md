# Bug 2989: SliceReadCommand uses wrong field for CF name, breaking super-column slices

**Commit**: ef2f631ebe · **Ticket**: n/a

## Detection Rule
When a ReadCommand subclass stores a compound "CF:supercolumn" string, ensure that `getColumnFamilyName()` parses only the CF portion rather than returning the full compound string.

## Code Smell
A field named `columnFamily` stores either a plain CF name or a "CF:supercolumn" compound string; `getColumnFamilyName()` returns it verbatim without stripping the supercolumn part, causing lookups to fail for super-column slices.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: compound-field-not-parsed
- severity: crash
- subsystem: cql-query

## Root Cause
`SliceReadCommand` stored the full "CF:supercolumn" identifier in a field named `columnFamily` and returned it directly from `getColumnFamilyName()`. For standard CF slices this happened to be correct, but for super-column slices the returned value included the super-column name, so table lookups failed. The fix renames the field to `columnFamilyColumn` (making the dual-use obvious) and implements `getColumnFamilyName()` to split on ":" and return only the first token.

## Example
```java
// BEFORE: returns "CF:supercolumn" for super CF slices
public String getColumnFamilyName() {
    return columnFamily; // wrong for super-column case
}

// AFTER: always returns just the CF name
public String getColumnFamilyName() {
    return RowMutation.getColumnAndColumnFamily(columnFamilyColumn)[0];
}
```
