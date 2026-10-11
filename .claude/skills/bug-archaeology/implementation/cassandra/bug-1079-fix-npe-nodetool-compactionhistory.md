# Bug 1079: Fix NPE in nodetool compactionhistory

**Commit**: 588dc06eb8 · **Ticket**: CASSANDRA-9758
**Subsystem**: src/java/org/apache/cassandra/utils/FBUtilities.java
**Category**: Missing null / bounds check

---

## Root Cause

`FBUtilities.toString(Map)` was called by the compaction history display code to format the compaction parameters map stored in the system table. When a compaction was recorded without parameters (i.e., the map column was null), the method received `null` and immediately passed it to `Joiner.on(...).withKeyValueSeparator(...).join(null)`, which threw a `NullPointerException`. The method had no null guard; its contract was implicitly "non-null map required" even though callers could legitimately pass null.

### Before / After

```java
// BEFORE
public static String toString(Map<?,?> map)
{
    Joiner.MapJoiner joiner = Joiner.on(", ").withKeyValueSeparator(":");
    return joiner.join(map);
}
```

```java
// AFTER
@Nonnull
public static String toString(@Nullable Map<?, ?> map)
{
    if (map == null)
        return "";
    Joiner.MapJoiner joiner = Joiner.on(", ").withKeyValueSeparator(":");
    return joiner.join(map);
}
```

---

## How to Spot

Utility methods that format or serialize data and delegate to a library that does not accept null (such as Guava's `Joiner`) need an explicit null guard if callers can pass null. Reviewers should trace every path that feeds data from a database read or network message into a formatting utility and confirm whether a null value is possible at the source, then verify the utility handles it.

---

## Test

`FBUtilitiesTest.testToString` was added, verifying that `null` returns an empty string, that an empty map returns an empty string, and that single and multi-entry maps format correctly.
