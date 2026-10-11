# Bug 1107: Fix Double Flushing of Secondary Index Tables

**Commit**: b8172dcaa8 · **Ticket**: CASSANDRA-10180
**Subsystem**: src/java/org/apache/cassandra/index/SecondaryIndexManager.java
**Category**: Logic error in condition

---

## Root Cause

`SecondaryIndexManager.flushIndexesBlocking` used `Optional.orElse` to handle indexes that have no backing CFS (non-CFS indexes). `orElse` always evaluates its argument, even when the `Optional` is non-empty. Because `wait.add(cfs.forceFlush())` already returns a value (a `Future`), the whole expression evaluates to a non-null `Optional` value, but the `orElse(nonCfsIndexes.add(index))` side-effect was also executed unconditionally — causing every index to be added to `nonCfsIndexes` regardless of whether it had a backing table, resulting in a double flush. The correct idiom for a conditional side-effect on an absent `Optional` is `orElseGet`, which only evaluates the supplier when the `Optional` is empty.

### Before / After

```java
// BEFORE
index.getBackingTable()
     .map(cfs -> wait.add(cfs.forceFlush()))
     .orElse(nonCfsIndexes.add(index));
```

```java
// AFTER
index.getBackingTable()
     .map(cfs -> wait.add(cfs.forceFlush()))
     .orElseGet(() -> nonCfsIndexes.add(index));
```

---

## How to Spot

Any `optional.orElse(sideEffect())` pattern is a bug when the intent is to conditionally run the side-effect only in the absent case. `orElse` eagerly evaluates its argument regardless of the `Optional`'s state; `orElseGet` lazily evaluates. During code review, scrutinize every `orElse` whose argument is a method call with a side effect (collection mutation, I/O, etc.) and replace with `orElseGet`.

---

## Test

No test added. The bug is a code-pattern issue clearly visible in review.
