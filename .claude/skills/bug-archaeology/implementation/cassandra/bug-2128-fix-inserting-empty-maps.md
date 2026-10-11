# Bug 2128: CQL Parser Cannot Distinguish Empty Map from Empty Set Literal

**Commit**: 189131631b · **Ticket**: CASSANDRA-5141
**Subsystem**: src/java/org/apache/cassandra/cql3/operations/SetOperation.java, src/java/org/apache/cassandra/cql3/statements/UpdateStatement.java
**Category**: Logic error in condition

---

## Root Cause

The CQL3 parser cannot syntactically distinguish between an empty set literal `{}` and an empty map literal `{}` — both parse identically. When the parser encounters `{}` it always produces a `SetOperation`, so assigning an empty map to a map column would be mishandled: the operation type remained `SET` even when the target column was declared as a `MapType`. The fix adds a post-parse conversion step in `UpdateStatement` that inspects the actual column type and, when the target is a `MapType` and the operation is an empty `SetOperation`, replaces it with the correct `MapOperation`.

### Before / After

```java
// BEFORE — always stores as SetOperation regardless of column type
processedColumns.put(name, operation);
```

```java
// AFTER — converts empty set operation to empty map operation when column is MapType
private void addNewOperation(CFDefinition.Name name, Operation operation)
{
    if (operation.getType() == Operation.Type.SET && (name.type instanceof MapType))
        operation = ((SetOperation)operation).maybeConvertToEmptyMapOperation();
    processedColumns.put(name, operation);
}
```

---

## How to Spot

Watch for parsers that share a single AST node type for multiple semantically distinct constructs that only differ when context (the schema type) is known. Any place where the correct interpretation is deferred until type resolution should have an explicit coercion step at the point where types are first available.

---

## Test

No test added in this commit; the fix is exercised by the manual scenario of inserting `{}` into a map column.
