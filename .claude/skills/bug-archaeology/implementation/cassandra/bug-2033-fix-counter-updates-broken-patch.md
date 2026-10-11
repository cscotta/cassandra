# Bug 2033: Counter Table Updates Broken by Misplaced hasConditions() Guard

**Commit**: 2f3f620ed1 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/ModificationStatement.java
**Category**: Logic error in condition

---

## Root Cause

A prior patch (CASSANDRA-5443) added a `hasConditions()` check after the conditional-update parsing block, but the counter-table guard (`isCounter()`) and the custom-timestamp guard were placed inside that block unconditionally. This meant that for plain counter updates (no `IF` clause), the `isCounter()` rejection path was never reached, allowing malformed counter mutations to proceed. The fix wraps the entire conditional-update section inside `if (stmt.hasConditions())`, so all guard checks only apply when conditions are actually present.

### Before / After

```java
// BEFORE
if (ifNotExists) { ... }
else {
    for (Pair<...> entry : conditions) { ... }
}
if (stmt.hasConditions() && attrs.timestamp != null)
    throw new InvalidRequestException("Cannot provide custom timestamp for conditional update");
```

```java
// AFTER
if (stmt.hasConditions()) {
    if (stmt.isCounter())
        throw new InvalidRequestException("Conditional updates are not supported on counter tables");
    if (attrs.timestamp != null)
        throw new InvalidRequestException("Cannot provide custom timestamp for conditional update");
    if (ifNotExists) { ... }
    else {
        for (Pair<...> entry : conditions) { ... }
    }
}
```

---

## How to Spot

When adding guards that must apply only in a specific execution branch, ensure those guards live inside the branch rather than after it. Post-hoc guard checks (placed after the block they relate to) are error-prone if the block mutates state that the guard depends on.

---

## Test

No test added.
