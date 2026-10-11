# Bug 2279: Null keyAlias Passed Unconditionally in CQL2 CREATE TABLE

**Commit**: 70e318eaa9 · **Ticket**: n/a

## Detection Rule

When a schema builder method accepts a list of aliases that may legitimately be empty or null (e.g., in an older CQL dialect), verify that the call is guarded with a null/empty check rather than always invoking the setter.

## Code Smell

`newCFMD.keyAliases(Collections.singletonList(keyAlias))` is called unconditionally inside a builder chain even when `keyAlias` can be `null`, producing a singleton list containing `null`.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: unconditional-null-list-entry
- severity: crash
- subsystem: schema-metadata

## Root Cause

In CQL2, using the literal `key` as the primary key column name produced a `null` `keyAlias` because the grammar did not extract an alias in that case. The builder chain called `.keyAliases(Collections.singletonList(null))` unconditionally, injecting a null entry into the metadata and causing downstream NPEs. The fix guards the alias setter with `if (keyAlias != null)`.

## Example

```java
// BEFORE
newCFMD
    .keyAliases(Collections.<ByteBuffer>singletonList(keyAlias))  // keyAlias may be null
    .compactionStrategyClass(...)
```

```java
// AFTER
newCFMD
    .compactionStrategyClass(...);

// CQL2 can have null keyAliases
if (keyAlias != null)
    newCFMD.keyAliases(Collections.<ByteBuffer>singletonList(keyAlias));
```
