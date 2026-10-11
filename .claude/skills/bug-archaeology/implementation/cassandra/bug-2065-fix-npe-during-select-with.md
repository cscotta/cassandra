# Bug 2065: NPE in CQL3 SELECT When an Undefined Function Is Used as an Argument

**Commit**: b82027ccbc · **Ticket**: CASSANDRA-5404
**Subsystem**: src/java/org/apache/cassandra/cql3/functions/FunctionCall.java, src/java/org/apache/cassandra/cql3/statements/Selection.java
**Category**: Missing null / bounds check

---

## Root Cause

`Functions.getReturnType()` returns `null` when the named function does not exist. Two call sites failed to handle this: `Selection.makeSelector()` passed the null directly to `makeFunctionSpec()` which then dereferenced it, causing an NPE; and `FunctionCall.Unprepared.isAssignableTo()` called `.asCQL3Type()` on the null return type when checking argument compatibility. The fix adds an explicit null-check in `Selection` that throws a helpful `InvalidRequestException`, and a null-guard in `isAssignableTo()` that returns `true` (deferring the "unknown function" error to the later, more informative path).

### Before / After

```java
// BEFORE – Selection.java, no null check
AbstractType<?> returnType = Functions.getReturnType(withFun.functionName, ...);
ColumnSpecification spec = makeFunctionSpec(cfDef, withFun, returnType); // NPE if null
```

```java
// AFTER
AbstractType<?> returnType = Functions.getReturnType(withFun.functionName, ...);
if (returnType == null)
    throw new InvalidRequestException(String.format("Unknown function '%s'", withFun.functionName));
ColumnSpecification spec = makeFunctionSpec(cfDef, withFun, returnType);
```

```java
// BEFORE – FunctionCall.java isAssignableTo
return receiver.type.asCQL3Type().equals(returnType.asCQL3Type()); // NPE if returnType null
```

```java
// AFTER
return returnType == null || receiver.type.asCQL3Type().equals(returnType.asCQL3Type());
```

---

## How to Spot

Any lookup that can return `null` for "not found" must be null-checked before every dereference. Registry-lookup results (`getReturnType`, `get`, `find`) are especially risky when callers pass user-supplied names, because valid production data will never trigger the path but malformed queries will.

---

## Test

No test added.
