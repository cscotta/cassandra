# Bug 2010: Hadoop InputKeyRange Logic Inverted, Key vs Token Confusion in CFIF

**Commit**: aaf18bd08a · **Ticket**: CASSANDRA-5536
**Subsystem**: src/java/org/apache/cassandra/hadoop/ColumnFamilyInputFormat.java
**Category**: Logic error in condition

---

## Root Cause

`ColumnFamilyInputFormat.getSplits()` checked `jobKeyRange.start_token != null` to decide whether to apply a key range restriction, but it should check `start_key`. When a job range was configured using keys (not tokens), the null token caused the range to be ignored entirely. Worse, when a token was present, the code fetched `fromString(start_token)` — treating the token string as the range endpoint — instead of calling `partitioner.getToken(start_key)` to derive the token from the key. The fix restructures the condition to branch on `start_key` first, validates that the key/token fields are used exclusively, and correctly derives the token from the key via `partitioner.getToken()`.

### Before / After

```java
// BEFORE
if (jobKeyRange != null && jobKeyRange.start_token != null)
{
    assert partitioner.preservesOrder() ...;
    assert jobKeyRange.start_key == null ...;
    jobRange = new Range<Token>(
        partitioner.getTokenFactory().fromString(jobKeyRange.start_token),
        partitioner.getTokenFactory().fromString(jobKeyRange.end_token),
        partitioner);
}
```

```java
// AFTER
if (jobKeyRange != null)
{
    if (jobKeyRange.start_key == null) {
        logger.warn("ignoring jobKeyRange specified without start_key");
    } else {
        if (!partitioner.preservesOrder())
            throw new UnsupportedOperationException(...);
        if (jobKeyRange.start_token != null)
            throw new IllegalArgumentException("only start_key supported");
        jobRange = new Range<Token>(
            partitioner.getToken(jobKeyRange.start_key),
            partitioner.getToken(jobKeyRange.end_key),
            partitioner);
    }
}
```

---

## How to Spot

When an API object has parallel fields that are mutually exclusive (`start_key` vs `start_token`), guard conditions must branch on the intended primary field, not on the secondary. Check that the factory method used to create range endpoints matches the field type being consumed — using `getTokenFactory().fromString(token)` instead of `getToken(key)` silently produces a wrong range endpoint.

---

## Test

No test added.
