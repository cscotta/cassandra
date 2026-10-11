# Bug 1689: Fix Unintended Update with Conditional Statement

**Commit**: 167380fb0d · **Ticket**: CASSANDRA-6893
**Subsystem**: src/java/org/apache/cassandra/cql3/ColumnCondition.java
**Category**: Logic error in condition

---

## Root Cause

`ColumnCondition` stored the bound variable list as mutable state on the object itself (`attach(variables)`), and `CQL3CasConditions` reused the same condition objects across protocol batches where each condition could have a different variable list. This meant the last-assigned variable list would silently overwrite an earlier one, causing condition evaluation to use the wrong bindings and potentially applying a write when the condition should have failed. The fix introduced an inner `WithVariables` class that pairs a condition with its variable list immutably, so each evaluation gets its own isolated binding.

Additionally, `listAppliesTo` had a logic inversion (`!iter.next().value().equals(e)` should negate to report non-equality, but the negation was missing the `!`), which caused list equality checks to pass when elements did not match.

### Before / After

```java
// BEFORE – mutable variable attachment, shared across batch conditions
public ColumnCondition attach(List<ByteBuffer> variables) {
    this.variables = variables;
    return this;
}
// ...
for (ByteBuffer e : elements)
    if (!iter.hasNext() || iter.next().value().equals(e))  // missing negation
        return false;
```

```java
// AFTER – immutable wrapper, isolated per evaluation
public ColumnCondition.WithVariables with(List<ByteBuffer> variables) {
    return new WithVariables(variables);
}
// ...
for (ByteBuffer e : elements)
    if (!iter.hasNext() || !iter.next().value().equals(e))  // correct negation
        return false;
```

---

## How to Spot

When an object carries both mutable state (variable bindings) and evaluation logic that is called from multiple call sites with different state values, an unintended update can occur if the state from a later call overwrites an earlier one. Prefer value-object wrappers that capture state at creation time.

---

## Test

No new test added in this commit.
