# Bug 2020: NPE in Pig Wide-Row Mode When `addKeyToTuple` Return Value Ignored

**Commit**: 7d2ce5f957 · **Ticket**: CASSANDRA-5488
**Subsystem**: src/java/org/apache/cassandra/hadoop/pig/CassandraStorage.java
**Category**: Missing null / bounds check

---

## Root Cause

`CassandraStorage.addKeyToTuple` was refactored to return the (possibly newly created) `Tuple`, but several call sites in the wide-row `next()` loop discarded the return value. When `tuple` was `null` on entry, the method allocated a new `Tuple` internally, but the caller's local variable still held `null`. Any subsequent attempt to add columns or return the tuple produced a `NullPointerException`. The fix captures the return value at every call site.

### Before / After

```java
// BEFORE — returned tuple dropped, local variable remains null
addKeyToTuple(tuple, lastKey, cfDef, parseType(cfDef.getKey_validation_class()));
```

```java
// AFTER — returned tuple captured
tuple = addKeyToTuple(tuple, lastKey, cfDef, parseType(cfDef.getKey_validation_class()));
```

---

## How to Spot

When a method both mutates a parameter and returns a (potentially different) object, callers must always capture the return value. Code review should flag any call to such a method where the return value is discarded — a common pattern after refactoring a `void` method into one that can also allocate.

---

## Test

No test added (pig test script updated to use `widerows=true` to expose the path).
