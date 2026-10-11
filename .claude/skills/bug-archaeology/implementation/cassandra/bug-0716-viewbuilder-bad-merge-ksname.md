# Bug 0716: Bad merge — stale metadata field reference in ViewBuilder logging

**Commit**: 5d8b5cce49 · **Ticket**: n/a
**Subsystem**: Materialized views / ViewBuilder
**Category**: Wrong constant / default value

---

## Root Cause
A prior merge left conflicting logging statements in `ViewBuilder.run()`: one using the current field name `baseCfs.metadata.keyspace` and a duplicate using the removed field `baseCfs.metadata.ksName`. The stale `ksName` references would cause a compilation failure or a `NoSuchFieldError` at runtime. The fix removes the duplicate log lines and standardises all four call sites on the current `metadata.keyspace` accessor.

### Before / After
```java
// BEFORE — duplicate lines; one with stale field name
logger.debug("Starting view builder for {}.{}", baseCfs.metadata.ksName, view.name);
logger.trace("Running view builder for {}.{}", baseCfs.metadata.keyspace, view.name);

// AFTER — single line using current field
logger.debug("Starting view builder for {}.{}", baseCfs.metadata.keyspace, view.name);
```

---

## How to Spot
After a merge that renames or removes a field, a quick `grep` for the old name across modified files catches stale references before they reach a compiler or runtime. Duplicate adjacent log lines with different log levels but the same message text are a tell-tale sign of a bad merge that preserved both sides of a conflict.

---

## Test
No test added.
