# Bug 2090: Inverted Short-Read Retry Condition in SliceFromReadCommand

**Commit**: ad980210a9 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/SliceFromReadCommand.java
**Category**: Logic error in condition

---

## Root Cause

A bad merge inverted the guard condition that decides whether Cassandra needs to issue a short-read retry. The intent is: retry only if at least one replica returned a full `count` of live columns but the merged result is still short — signalling that some columns may have been shadowed by tombstones elsewhere. The merged code used `>= count` (fire retry when enough columns were returned) instead of `< count` (fire retry when columns are still lacking), causing retries to happen in the wrong direction: no retry when genuinely short, and spurious work when results were already sufficient.

### Before / After

```java
// BEFORE (bad merge)
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

Inverted boolean guards are a classic merge-conflict artifact. When reviewing merge fixes, check that every changed comparison operator still matches the surrounding comment's stated intent.

---

## Test

No test added.
