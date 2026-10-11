# Bug 2258: Assertion Error Preparing Parameterless CQL3 Query via Binary Protocol

**Commit**: 565f7e7802 · **Ticket**: CASSANDRA-4577
**Subsystem**: src/java/org/apache/cassandra/cql3/ResultSet.java
**Category**: Missing null / bounds check

---

## Root Cause

`ResultSet.Metadata` called `allInSameCF()` to decide whether to set the `GLOBAL_TABLES_SPEC` flag, but `allInSameCF()` iterates over the names list and accesses the first element without guarding against an empty list. When a CQL3 query has no bound parameters (e.g., a plain `SELECT` with no `?` markers), the names list passed to the constructor is empty, causing an assertion error (index-out-of-bounds or empty-iterator assertion) inside `allInSameCF()`. The fix adds an `!names.isEmpty()` guard so the flag is only evaluated when there is at least one column specification.

### Before / After

```java
// BEFORE
if (allInSameCF())
    flags.add(Flag.GLOBAL_TABLES_SPEC);
```

```java
// AFTER
if (!names.isEmpty() && allInSameCF())
    flags.add(Flag.GLOBAL_TABLES_SPEC);
```

---

## How to Spot

Any helper method that inspects the first element of a collection should be guarded by an emptiness check at every call site, especially in constructors that may be invoked with an empty collection. Look for calls to `allInSameCF()` or similar "all-of-collection" predicates that lack an upfront empty-list guard.

---

## Test

No test added.
