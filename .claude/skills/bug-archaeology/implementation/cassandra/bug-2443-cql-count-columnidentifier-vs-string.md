# Bug 2443: CQL SELECT COUNT(*) validation compares ColumnIdentifier object to String literal

**Commit**: f5f3065843 · **Ticket**: CASSANDRA-3787

## Detection Rule

When validating a parsed token against a string literal, ensure `.toString()` is called before the comparison if the token is a wrapper object whose `equals` compares identity or type.

## Code Smell

`selectClause.get(0).equals("*")` where the list element is a `ColumnIdentifier` (not a `String`); the comparison always returns false, making COUNT(*) and COUNT(1) always fail validation.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: object-vs-string-literal-equals
- severity: crash
- subsystem: cql-query

## Root Cause

`SelectStatement` validation for `COUNT(*)` checked `selectClause.get(0).equals("*")` but `selectClause` held `ColumnIdentifier` objects. `ColumnIdentifier.equals(Object)` is not symmetric with `String.equals`, so the comparison always returned false and threw `InvalidRequestException` even for valid `COUNT(*)` / `COUNT(1)` queries. The fix calls `.toString()` to get the text before comparing.

## Example

```java
// BEFORE – ColumnIdentifier never equals a String
if (selectClause.size() != 1
        || (!selectClause.get(0).equals("*") && !selectClause.get(0).equals("1")))
    throw new InvalidRequestException("Only COUNT(*) and COUNT(1) operations are supported.");

// AFTER
if (selectClause.size() != 1)
    throw new InvalidRequestException("Only COUNT(*) and COUNT(1) operations are supported.");
String columnName = selectClause.get(0).toString();
if (!columnName.equals("*") && !columnName.equals("1"))
    throw new InvalidRequestException("Only COUNT(*) and COUNT(1) operations are supported.");
```
