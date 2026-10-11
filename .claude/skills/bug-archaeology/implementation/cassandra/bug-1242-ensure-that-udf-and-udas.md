# Bug 1242: Ensure that UDF and UDAs are keyspace-isolated

**Commit**: 6eea3ea395 · **Ticket**: CASSANDRA-9409
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/CreateAggregateStatement.java, CreateFunctionStatement.java, CQL3Type.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When creating a User-Defined Aggregate (UDA), the `SFUNC` and `FINALFUNC` clauses accepted plain unqualified names that were silently resolved by prepending the aggregate's own keyspace. However, if a fully-qualified name from a different keyspace was provided, no cross-keyspace check was performed, allowing a UDA in keyspace A to reference a UDF defined in keyspace B. The same gap existed in `CreateFunctionStatement` for type resolution: argument and return types were prepared using the type's own keyspace rather than the function's keyspace, permitting cross-keyspace user type references. The root cause is that keyspace isolation, already enforced for user types in non-UDF statements, was never wired into the UDF/UDA validation path.

### Before / After

```java
// BEFORE – stateFunc was a plain String, blindly prepended with the aggregate's keyspace
FunctionName stateFuncName = new FunctionName(functionName.keyspace, stateFunc);
Function f = Functions.find(stateFuncName, stateArguments(stateType, argTypes));
```

```java
// AFTER – stateFunc is a FunctionName; cross-keyspace use throws InvalidRequestException
private FunctionName validateFunctionKeyspace(FunctionName func)
{
    if (!func.hasKeyspace())
        return new FunctionName(functionName.keyspace, func.name);
    else if (!SystemKeyspace.NAME.equals(func.keyspace) && !functionName.keyspace.equals(func.keyspace))
        throw new InvalidRequestException(
            String.format("Statement on keyspace %s cannot refer to a user function in keyspace %s; ...",
                          functionName.keyspace, func.keyspace));
    return func;
}
```

---

## How to Spot

Whenever a new CQL statement accepts identifiers (function names, type names, index names) as arguments, verify that every such identifier is validated against the statement's own keyspace before lookup. The pattern to watch for is a bare `String` name field that is only qualified at the point of lookup rather than at parse or validation time — converting such fields to fully-qualified value objects (like `FunctionName`) and adding an explicit cross-keyspace guard is the correct remedy. A reviewer should also check whether the system keyspace needs a carve-out (built-in functions live there and must remain universally accessible).

---

## Test

Tests added in `AggregationTest.testWrongKeyspace()` and `AggregationTest.testSystemKeyspace()` and `UFTest.testWrongKeyspace()`: they assert that cross-keyspace UDF/UDA references are rejected with the appropriate error message, and that references to system-keyspace built-in functions are still permitted.
