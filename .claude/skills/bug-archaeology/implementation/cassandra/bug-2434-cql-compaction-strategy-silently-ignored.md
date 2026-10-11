# Bug 2434: CQL CREATE TABLE silently ignores compaction_strategy_class option

**Commit**: f4064b5ff1 · **Ticket**: CASSANDRA-3374 (follow-up)

## Detection Rule

When a builder-pattern DDL statement parses options into fields, verify that every parsed field is actually forwarded to the builder call that constructs the final metadata object.

## Code Smell

A property (e.g., `compactionStrategyClass`) is parsed and stored in a local field during `validate()` but the builder call in `getCFMetaData()` never passes that field, silently falling back to the default.

## Tags

- scope: cassandra
- category: silent-skip
- code-pattern: parsed-field-not-forwarded
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause

`CreateColumnFamilyStatement` parsed `compaction_strategy_class` during validation but never stored the resolved class or passed it to the `CFMetaData` builder. The table was always created with the default compaction strategy regardless of the user's specification. The fix adds a `compactionStrategyClass` field, resolves it in `validate()`, and passes it via `.compactionStrategyClass(compactionStrategyClass)` in the builder chain.

## Example

```java
// BEFORE – field parsed but never forwarded
private void validate() throws InvalidRequestException {
    // compaction_strategy_class read but not stored
}
// builder omits compactionStrategyClass:
return new CFMetaData(...)
    .compactionStrategyOptions(compactionStrategyOptions)
    ...

// AFTER
private Class<? extends AbstractCompactionStrategy> compactionStrategyClass;
private void validate() throws InvalidRequestException {
    compactionStrategyClass = CFMetaData.createCompactionStrategy(getPropertyString(...));
}
// builder includes it:
return new CFMetaData(...)
    .compactionStrategyClass(compactionStrategyClass)
    .compactionStrategyOptions(compactionStrategyOptions)
    ...
```
