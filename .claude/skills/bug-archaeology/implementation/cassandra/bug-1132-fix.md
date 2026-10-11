# Bug 1132: CREATE AGGREGATE Incorrectly Resolves Function Keyspace

**Commit**: fec40fd43b · **Ticket**: CASSANDRA-10040 (fixes CASSANDRA-9771)
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/CreateAggregateStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CREATE AGGREGATE` accepted fully qualified `keyspace.functionname` syntax for `SFUNC` and `FINALFUNC` even though the CQL grammar did not legitimately support it — the grammar rule used `functionName` (which allows a keyspace prefix) instead of `allowedFunctionName` (which allows only an unqualified name). The `validateFunctionKeyspace` helper then silently allowed cross-keyspace references to system functions such as `system.min`, meaning a user could reference functions outside the aggregate's own keyspace. The fix changes the grammar to use `allowedFunctionName` for both clauses and removes the permissive `validateFunctionKeyspace` method, instead always inheriting the keyspace from the aggregate's own name at construction time.

### Before / After

```antlr
// BEFORE: grammar accepted qualified names for state/final func
K_SFUNC sfunc = functionName
K_STYPE stype = comparatorType
( K_FINALFUNC ffunc = functionName )?
```

```antlr
// AFTER: grammar restricts to unqualified names only
K_SFUNC sfunc = allowedFunctionName
K_STYPE stype = comparatorType
( K_FINALFUNC ffunc = allowedFunctionName )?
```

```java
// BEFORE: validateFunctionKeyspace allowed cross-keyspace refs including system.*
private FunctionName validateFunctionKeyspace(FunctionName func)
{
    if (!func.hasKeyspace())
        return new FunctionName(functionName.keyspace, func.name);
    else if (!SystemKeyspace.NAME.equals(func.keyspace) && !functionName.keyspace.equals(func.keyspace))
        throw new InvalidRequestException(...);
    return func;  // system.* allowed through
}
```

```java
// AFTER: keyspace always inherited from the aggregate, no cross-keyspace escape
this.stateFunc = new FunctionName(functionName.keyspace, stateFunc);
this.finalFunc = finalFunc != null ? new FunctionName(functionName.keyspace, finalFunc) : null;
```

---

## How to Spot

When a grammar rule offers two alternatives — one that accepts qualified names and one that accepts only simple names — review every call site to confirm the right alternative is used for each clause. A `CREATE` statement for an object scoped to a keyspace should never silently accept function references from a different keyspace via grammar ambiguity. Also review helper methods that validate namespacing: an explicit allowlist of "permitted foreign namespaces" (here `system.*`) is a sign the rule is too permissive and the fix should be pushed into the grammar itself.

---

## Test

`AggregationTest` was updated to assert that specifying a qualified `keyspace.functionname` for `SFUNC` or `FINALFUNC` is now a syntax error ("mismatched input" / "missing EOF"), and a new case verifies that `system.min` is no longer a permitted `FINALFUNC` reference from another keyspace.
