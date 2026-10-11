# Bug 2515: Super column family CFDef printed with wrong comparator due to `==` on String

**Commit**: 8987f6fa4d · **Ticket**: CASSANDRA-3415

## Detection Rule
When comparing a String field to a literal (such as `"Super"`), always use `.equals()` rather than `==`, which tests reference identity and will silently return `false` for non-interned Strings coming from network deserialization.

## Code Smell
`cfDef.column_type == "Super"` — `column_type` is a Thrift-deserialized `String` and will not share the same reference as the literal `"Super"`, so the condition is always `false` and the `subcomparator_type` is never selected, causing super column family entries to be displayed with the wrong comparator.

## Tags
- scope: java
- category: wrong-filter-result
- code-pattern: reference-equality-on-string
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
The CLI uses `cfDef.column_type` to decide whether to use `subcomparator_type` (for super CFs) or `comparator_type` when formatting column metadata. Using `==` instead of `.equals()` means the branch always takes the `comparator_type` path, so super column family column names are decoded with the wrong type and appear as raw hex or garbage in `describe keyspace` / `show schema` output.

## Example
```java
// BEFORE
final AbstractType comparator = getFormatType(
    (cfDef.column_type == "Super")      // reference equality — always false
        ? cfDef.subcomparator_type
        : cfDef.comparator_type);
```
```java
// AFTER
final AbstractType comparator = getFormatType(
    cfDef.column_type.equals("Super")   // value equality
        ? cfDef.subcomparator_type
        : cfDef.comparator_type);
```
