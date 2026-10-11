# Bug 1465: Fix Token Range Query Validation With Multiple Partition Key Columns

**Commit**: b1166c0998 · **Ticket**: CASSANDRA-6075
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Logic error in condition

---

## Root Cause

`checkTokenFunctionArgumentsOrder` validated that each relation in a `WHERE token(...) > token(...)` clause referred to partition key columns in the correct order by iterating a plain `Iterator` over the partition key column list. For a compound partition key with two token relations (e.g. `token(a,b) > token(0,'a') AND token(a,b) < token(0,'d')`), the iterator was exhausted by the first relation and produced no elements for the second, so the second clause's column order was never checked. The guard `cfDef.partitionKeyCount() > 0` also incorrectly suppressed the check for single-partition-key tables when `onToken` was true.

### Before / After

```java
// BEFORE
if (stmt.onToken && cfDef.partitionKeyCount() > 0)
    checkTokenFunctionArgumentsOrder(cfDef);
...
Iterator<Name> iter = cfDef.partitionKeys().iterator();
```

```java
// AFTER
if (stmt.onToken)
    checkTokenFunctionArgumentsOrder(cfDef);
...
Iterator<Name> iter = Iterators.cycle(cfDef.partitionKeys());
```

---

## How to Spot

Whenever a validation loop consumes a shared iterator across multiple input items, the iterator will be exhausted mid-loop and silently skip validation for the remaining items. Use `Iterators.cycle` or re-create the iterator per item when each item independently needs to traverse the full reference list.

---

## Test

New test cases in `SelectWithTokenFunctionTest`: a valid range query with both `>=` and `<` bounds on a single-column key, a valid range on a compound key, and negative tests (`testTokenFunctionWithTwoGreaterThan`, `testTokenFunctionWithGreaterThanAndEquals`, `testTokenFunctionOnEachPartitionKeyColumns`) that assert `InvalidRequestException` for malformed token predicates.
