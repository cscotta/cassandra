# Bug 2256: NPE in CQL3 SELECT When Column Has Both Equality and Inequality Restrictions

**Commit**: 8e03f7bdf2 · **Ticket**: CASSANDRA-4532
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Missing null / bounds check

---

## Root Cause

In `SelectStatement`, restriction processing computes a `bounds` array for range predicates on clustering columns. When a column is restricted by both an equality (`=`) and an inequality (`<`, `>`, etc.), the code path that handles the inequality does not produce a `bounds` array (it returns `null`), but the subsequent code unconditionally dereferenced `bounds[b.idx]` without checking if `bounds` itself was null. The fix adds a null check that also turns the silent NPE into a meaningful `InvalidRequestException` explaining the conflicting restrictions.

### Before / After

```java
// BEFORE
// bounds may be null if column already has an equality restriction
if (bounds[b.idx] != null)   // NPE when bounds is null
    throw new InvalidRequestException(...);
bounds[b.idx] = t;
```

```java
// AFTER
if (bounds == null)
    throw new InvalidRequestException(String.format(
        "%s cannot be restricted by both an equal and an inequal relation", name));

if (bounds[b.idx] != null)
    throw new InvalidRequestException(...);
bounds[b.idx] = t;
```

---

## How to Spot

When processing restriction objects that carry an index (`b.idx`) into a derived array, verify that the array itself was successfully allocated before indexing into it. Any code pattern of the form `array = compute(…); … array[idx]` without a null guard is a latent NPE if `compute` can return null for certain input combinations.

---

## Test

No test added.
