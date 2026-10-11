# Bug 1786: Fix IllegalArgumentException During Statement Preparation

**Commit**: a8a12d9e0a · **Ticket**: CASSANDRA-6592
**Subsystem**: src/java/org/apache/cassandra/cql3/statements
**Category**: Missing null / bounds check

---

## Root Cause

`SelectStatement`, `ModificationStatement`, and `BatchStatement` each implemented `measureForPreparedCache` by calling `meter.measureDeep(this)` and then subtracting the deep size of a shared/cached object (e.g. `cfDef` or `cfm`). `MemoryMeter.measureDeep` throws `IllegalArgumentException` when it encounters a reference to an object that has already been measured (to avoid cycles), so calling it on `this` — which transitively references those shared objects — caused the exception during prepared-statement size accounting. The fix replaces the deep-measure-and-subtract pattern with explicit shallow-measure of `this` plus individual `measureDeep` calls on each owned field, avoiding shared references entirely.

### Before / After

```java
// BEFORE (SelectStatement)
return meter.measureDeep(this) - meter.measureDeep(cfDef);

// BEFORE (ModificationStatement)
return meter.measureDeep(this) - meter.measureDeep(cfm);
```

```java
// AFTER (SelectStatement)
return meter.measure(this)
     + meter.measureDeep(parameters)
     + meter.measureDeep(selection)
     + ...owned fields only...;

// AFTER (ModificationStatement)
return meter.measure(this)
     + meter.measureDeep(attrs)
     + meter.measureDeep(processedKeys)
     + ...owned fields only...;
```

---

## How to Spot

`measureDeep` must never be called on an object that holds a reference to any already-measured object. Implementing `measureForPreparedCache` as `measureDeep(this) - measureDeep(shared)` is fragile; always decompose into `measure(this)` plus explicit `measureDeep` of each owned field.

---

## Test

No test added. The fix is observable by preparing a statement that previously threw `IllegalArgumentException`.
