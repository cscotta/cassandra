# Bug 1893: Fix merge — missing validators map declaration in AbstractCassandraStorage

**Commit**: b77fdcc3df · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hadoop/pig
**Category**: Missing null / bounds check

---

## Root Cause

A merge conflict resolution dropped the local variable declaration `Map<ByteBuffer,AbstractType> validators = getValidatorMap(cfDef);` from `AbstractCassandraStorage.columnToTuple()`. The subsequent `validators.get(col.name())` call referenced an undefined variable, causing a compilation error. The fix re-inserts the missing declaration at the correct point in the method before the first use of `validators`.

### Before / After

```java
// BEFORE (after bad merge — variable missing)
if (validators.get(col.name()) == null)
{
    Map<MarshallerType, AbstractType> marshallers = getDefaultMarshallers(cfDef);
    ...
}
```

```java
// AFTER
Map<ByteBuffer,AbstractType> validators = getValidatorMap(cfDef);
if (validators.get(col.name()) == null)
{
    Map<MarshallerType, AbstractType> marshallers = getDefaultMarshallers(cfDef);
    ...
}
```

---

## How to Spot

After resolving a merge conflict, always compile immediately. Local variable declarations are a common casualty of conflict resolution because they appear on their own line and are easy to delete when keeping one side of the conflict.

---

## Test

No test added.
