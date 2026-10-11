# Bug 2022: CQL3 CAS Conditions Check Used Wrong Predicate

**Commit**: 793d58a197 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/ModificationStatement.java
**Category**: Logic error in condition

---

## Root Cause

When preparing a CQL3 `IF` (Compare-And-Swap) modification statement, the guard that enforced CAS-specific restrictions (`isCounter` check, lightweight transaction flag, etc.) tested `stmt.hasConditions()` on the already-prepared statement object. However, `hasConditions()` on the prepared statement could return `false` in certain code paths even when the original parse included `IF NOT EXISTS` or explicit conditions, causing the validation block to be silently skipped. The fix replaces the post-preparation check with a direct inspection of the pre-preparation parse data (`ifNotExists` flag and `conditions` list), which is always accurate at that point.

### Before / After

```java
// BEFORE — checks the prepared stmt, which may not reflect parse-time conditions
if (stmt.hasConditions())
```

```java
// AFTER — checks raw parse inputs directly
if (ifNotExists || (conditions != null && !conditions.isEmpty()))
```

---

## How to Spot

When a validation guard needs to fire based on what the user wrote in the query, prefer checking the raw parsed representation rather than a derived property of the built object. A prepared/compiled object may have collapsed or transformed the original intent in ways that make presence-of-feature checks unreliable.

---

## Test

No test added.
