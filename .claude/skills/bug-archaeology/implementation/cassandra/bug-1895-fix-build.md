# Bug 1895: Fix build — unused import and broken error message format in PreparedQueryNotFoundException

**Commit**: d396fd47df · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/exceptions, src/java/org/apache/cassandra/thrift
**Category**: Logic error in condition

---

## Root Cause

`PreparedQueryNotFoundException.getMessage()` referenced `QueryProcessor.MAX_CACHE_PREPARED` in its format string with two `%d` / `%s` placeholders but the constant was removed from `QueryProcessor` in a prior refactor. This caused a compilation failure due to the unresolvable symbol. The thrift `CassandraServer` had an analogous inline error message with the same stale reference. The fix removes the import and replaces both format strings with a generic message that no longer tries to cite the (now-absent) cache-size constant.

### Before / After

```java
// BEFORE
return String.format("Prepared query with ID %s not found" +
    " or you have prepared more than %d queries and query %s has been evicted from the internal cache)",
    id, QueryProcessor.MAX_CACHE_PREPARED, id);
```

```java
// AFTER
return String.format("Prepared query with ID %s not found" +
    " or you have prepared too many queries and it has been evicted from the internal cache)",
    id);
```

---

## How to Spot

Format strings whose argument count does not match the number of `%` placeholders, or that reference constants from an external class, should be flagged during review. Removing a public constant requires a cross-codebase search for all uses.

---

## Test

No test added.
