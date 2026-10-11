# Bug 2058: Prepared BATCH Ignores Batch-Level Timestamp

**Commit**: 1429c54b01 · **Ticket**: CASSANDRA-5415
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/BatchStatement.java
**Category**: Logic error in condition

---

## Root Cause

`BatchStatement.executeWithoutConditions` applied the batch-level timestamp only when `isSetTimestamp()` returned true by calling `statement.setTimestamp()` as a side-effect before building mutations. However, `getMutations()` accepts a `timestamp` argument and uses it internally, so the two-step set-then-get approach broke for prepared statements where the timestamp binding happens at execution time. The fix removes the conditional `setTimestamp` call entirely and passes `getTimestamp(now)` directly to `getMutations()`, which always returns the correct value (either the explicitly set timestamp or the query-start time).

### Before / After

```java
// BEFORE
if (isSetTimestamp())
    statement.setTimestamp(getTimestamp(now));
for (IMutation m : statement.getMutations(variables, local, cl, now))
```

```java
// AFTER
for (IMutation m : statement.getMutations(variables, local, cl, getTimestamp(now)))
```

---

## How to Spot

When a method already accepts a parameter that encapsulates a decision (here: which timestamp to use), avoid redundant imperative side-effects that attempt to replicate the same decision. The side-effect path may be skipped in edge cases (e.g. prepared vs. inline) while the parameter path is always exercised.

---

## Test

No test added.
