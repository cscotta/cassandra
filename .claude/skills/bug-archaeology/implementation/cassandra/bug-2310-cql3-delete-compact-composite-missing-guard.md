# Bug 2310: CQL3 DELETE for composite tables missing primary key component check on compact tables

**Commit**: f04e39df73 · **Ticket**: CASSANDRA-4193

## Detection Rule

When validating that a composite primary key has all required parts specified, confirm that the guard condition covers all composite types (not just the non-compact form).

## Code Smell

A condition like `if (cfDef.isComposite && !cfDef.isCompact && builder.componentCount() != 0)` that incorrectly exempts compact composite tables from a validation that should apply to both compact and non-compact composites.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: missing-case-in-type-guard
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause

In `DeleteStatement`, when processing primary key components for a DELETE, if a component value was missing (`values == null || values.isEmpty()`), the code only enforced the "all-or-nothing" composite rule for non-compact sparse tables (`!cfDef.isCompact`). Compact composite tables were silently allowed to proceed with partial primary keys, producing incorrect deletion behavior. Additionally, when `columns.isEmpty()` (a DELETE * / full row delete), the code did not iterate over all column metadata to delete each column individually — it simply did nothing.

## Example

```java
// BEFORE
if (cfDef.isComposite && !cfDef.isCompact && builder.componentCount() != 0)
    throw new InvalidRequestException(...);
// ...
// Delete specific columns
Iterator<ColumnIdentifier> iter = columns.iterator();
```

```java
// AFTER
if (cfDef.isComposite && builder.componentCount() != 0)  // covers compact too
    throw new InvalidRequestException(...);
// ...
Iterator<ColumnIdentifier> iter;
if (columns.isEmpty())
    iter = cfDef.metadata.keySet().iterator();  // DELETE * — remove all
else
    iter = columns.iterator();
```
