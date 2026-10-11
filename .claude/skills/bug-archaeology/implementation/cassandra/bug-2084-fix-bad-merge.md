# Bug 2084: Inverted Short-Read Retry Condition in SliceFromReadCommand

**Commit**: 444a5515ba · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/SliceFromReadCommand.java
**Category**: Logic error in condition

---

## Root Cause

A bad merge introduced an inverted condition in `SliceFromReadCommand`'s short-read detection logic. The code is supposed to trigger a retry only when at least one replica returned the full requested `count` of live columns — meaning there may be more columns beyond what was merged. The pre-merge logic correctly checked `maxLiveColumns < count` to return `null` (no retry needed). After the bad merge, the condition became `maxLiveColumns >= count`, which returned `null` (no retry) precisely when a retry *was* warranted and scheduled a retry when it was not, causing incorrect query results.

### Before / After

```java
// BEFORE (bad merge)
// We generate a retry if at least one node reply with count live columns but after merge we have less
// than the total number of column we are interested in (which may be < count on a retry)
if (maxLiveColumns >= count)
    return null;
```

```java
// AFTER
// So in particular, if no host returned count live columns, we know it's not a short read.
if (maxLiveColumns < count)
    return null;
```

---

## How to Spot

Merge conflicts in conditional expressions are easy to miss because the code compiles and runs without error — only the behavior is wrong. When reviewing a merge-fix commit, look specifically for comparisons that were likely flipped (`<` vs `>=`, `==` vs `!=`) and verify the polarity matches the surrounding comment's intent.

---

## Test

No test added.
