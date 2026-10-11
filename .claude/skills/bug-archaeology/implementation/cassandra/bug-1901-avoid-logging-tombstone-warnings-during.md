# Bug 1901: Tombstone Warnings Incorrectly Fired During Compaction

**Commit**: 546cbf5ffc · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/filter, src/java/org/apache/cassandra/db/columniterator
**Category**: Missing override of default method

---

## Root Cause

`SliceQueryFilter` introduced a guard method `respectTombstoneFailures()` so that `IdentityQueryFilter` (used internally during compaction) could opt out of tombstone threshold checks. However, only the failure threshold check was gated by this method; the warning threshold check that logs a `WARN` message was not guarded at all. Furthermore, the method was renamed to `respectTombstoneThresholds()` to reflect its broader intent, but `IdentityQueryFilter`'s override still used the old name `respectTombstoneFailures()`, so the override was silently not called. The fix renames both the base method and the override consistently, and also gates the warning-level log behind the same guard.

### Before / After

```java
// BEFORE — warning not gated, and override name mismatch
if (respectTombstoneFailures() && columnCounter.ignored() > getTombstoneFailureThreshold()) { ... }

// unconditionally logs warning:
if (columnCounter.ignored() > DatabaseDescriptor.getTombstoneWarnThreshold())
    logger.warn(...);

protected boolean respectTombstoneFailures() { return true; }

// IdentityQueryFilter override — name matches but warn path still fires
@Override
protected boolean respectTombstoneFailures() { return false; }
```

```java
// AFTER — both checks gated, consistent method name
if (respectTombstoneThresholds() && columnCounter.ignored() > getTombstoneFailureThreshold()) { ... }

if (respectTombstoneThresholds() && columnCounter.ignored() > getTombstoneWarnThreshold())
    logger.warn(...);

protected boolean respectTombstoneThresholds() { return true; }

// IdentityQueryFilter
@Override
protected boolean respectTombstoneThresholds() { return false; }
```

---

## How to Spot

When adding a guard method to suppress a behavior, audit every code path that implements that behavior to ensure all branches (warn, error, exception) are gated. Also verify that every subclass that overrides the old method name is updated when the method is renamed.

---

## Test

No test added.
