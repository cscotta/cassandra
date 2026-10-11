# Bug 2018: Tuple Key Lost in Pig Wide-Row Iterator Due to Ignored Return Value

**Commit**: 2dd73d1710 · **Ticket**: CASSANDRA-5488 (round 2)
**Subsystem**: src/java/org/apache/cassandra/hadoop/pig/CassandraStorage.java
**Category**: Silent skip on non-match

---

## Root Cause

A prior fix (round 1) changed `addKeyToTuple` from a `void` method to one that returns a `Tuple`, but several call sites in the wide-row `next()` loop continued to discard the return value and used the original (un-keyed) tuple instead. The result was that wide-row tuples were returned without their row key populated. This round-2 fix separates concerns cleanly: `keyToTuple` creates a new keyed tuple, while `addKeyToTuple` becomes `void` again and mutates in place, eliminating the ambiguity of an ignored return value.

### Before / After

```java
// BEFORE (key silently lost — return value discarded)
tuple = TupleFactory.getInstance().newTuple();
...
addKeyToTuple(tuple, key, cfDef, parseType(...));  // return value ignored
```

```java
// AFTER (creation and mutation separated)
tuple = null;
...
if (tuple == null)
    tuple = keyToTuple(key, cfDef, parseType(...));
else
    addKeyToTuple(tuple, key, cfDef, parseType(...));
```

---

## How to Spot

When a method's return value is the same type as an argument it mutates, callers may silently ignore the return value while believing the mutation was sufficient. Prefer void mutation methods or pure factory methods; avoid methods that both mutate and return the same object.

---

## Test

No test added.
