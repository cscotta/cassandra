# Bug 1909: subList bounds error crashes AlwaysSpeculating read path with single replica

**Commit**: 7a87fc1186 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/AbstractReadExecutor.java
**Category**: Off-by-one

---

## Root Cause

`AlwaysSpeculatingReadExecutor.executeAsync()` (introduced by the speculative-retry refactor in the same release) unconditionally called `targetReplicas.subList(0, 2)` to send two data requests. When the replica list contained only one entry — a valid scenario when only one replica is available — this produced an `IndexOutOfBoundsException` because `subList(0, 2)` requires at least two elements. The fix clamps the upper bound to `min(2, targetReplicas.size())`.

### Before / After

```java
// BEFORE — always requests subList(0, 2), crashes with a single replica
makeDataRequests(targetReplicas.subList(0, 2));
```

```java
// AFTER — clamps upper bound to available replicas
makeDataRequests(targetReplicas.subList(0, targetReplicas.size() > 1 ? 2 : 1));
```

---

## How to Spot

Whenever a fixed-size subList slice is taken from a list whose size is not guaranteed, validate that the list is large enough. This is especially risky in code paths that are only exercised under specific topology configurations (e.g., single-node clusters or single-DC queries).

---

## Test

No test added.
