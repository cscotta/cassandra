# Bug 2239: CQL3 SELECT Allows IN Operator on Non-Primary-Key Indexed Columns (Branch Port)

**Commit**: ee4a26d865 · **Ticket**: CASSANDRA-4709
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

This commit applies the same fix as bug 2238 (commit 9c88ad16a6) to a different branch of `SelectStatement.java`. The query validation loop for secondary-indexed non-PK columns accepted multi-value `IN` restrictions as satisfying the "has indexed equality" requirement because `isEquality()` returns true for both single-value equality and `IN` lists. Without the check, an `IN` on a non-PK indexed column silently fell through to a single-key index lookup, returning only partial results instead of raising an error.

The fix in both branches is identical: after confirming `isEquality()`, check `restriction.eqValues.size() > 1` and throw `InvalidRequestException` before proceeding to the index name check.

### Before / After

```java
// BEFORE
if (entry.getValue().isEquality() && indexed.contains(entry.getKey().name.key))
{
    hasEq = true;
    break;
}
```

```java
// AFTER
if (!restriction.isEquality())
    continue;
if (restriction.eqValues.size() > 1)
    throw new InvalidRequestException(
        "Cannot use IN operator on column not part of the PRIMARY KEY");
if (indexed.contains(entry.getKey().name.key))
{
    hasEq = true;
    break;
}
```

---

## How to Spot

When the same validation gap exists across multiple active branches, a fix applied to only one branch leaves the other silently broken. During review of backport / cherry-pick commits, confirm that every branch containing the vulnerable code path receives the correction.

---

## Test

No test added.
