# Bug 2098: Assertion in SliceFromReadCommand Fires on Digest Mismatch Retry

**Commit**: 05709f74ed · **Ticket**: CASSANDRA-5284
**Subsystem**: src/java/org/apache/cassandra/db/SliceFromReadCommand.java
**Category**: Logic error in condition

---

## Root Cause

`SliceFromReadCommand` contained an assertion `assert maxLiveColumns <= count` followed by a retry condition that checked `maxLiveColumns == count`. During digest mismatch retries the speculative read path can return more live columns than originally requested (`count`), making the assertion fire spuriously. The assertion was wrong — `maxLiveColumns` can legitimately exceed `count` on a retry — and the equality check in the retry condition should have been `>=` to catch that case and still trigger the necessary re-read.

### Before / After

```java
// BEFORE
assert maxLiveColumns <= count;
if ((maxLiveColumns == count) && (liveColumnsInRow < getOriginalRequestedCount()))
```

```java
// AFTER
if ((maxLiveColumns >= count) && (liveColumnsInRow < getOriginalRequestedCount()))
```

---

## How to Spot

Assertions that express "this value will never exceed X" near comparison conditions that check for equality to X are suspicious. If the value can grow beyond the bound under concurrent or retry conditions, both the assertion and the equality comparison need to be loosened to `>=`.

---

## Test

No test added.
