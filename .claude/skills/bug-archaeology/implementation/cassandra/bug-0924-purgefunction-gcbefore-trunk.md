# Bug 0924: PurgeFunction Passes gcBefore as nowInSec, Causing Assertion Error During Compaction (Trunk)

**Commit**: d3f4ae8428 · **Ticket**: CASSANDRA-11353
**Subsystem**: Compaction / Tombstone purging
**Category**: Wrong constant / default value

---

## Root Cause
This is the trunk-branch application of the same fix as bug-0923. `PurgeFunction` stored and used `gcBefore` (the GC cutoff epoch) where `nowInSec` (the current time) was required for TTL-expiry evaluation. Passing `gcBefore` to `Row.purge()` caused an assertion error when `gcBefore` equalled `localDeletionTime`. A concurrent secondary bug in `AbstractCell.toTombstone()` omitted `path()` when converting expiring collection cells to tombstones, producing tombstones with null paths that subsequently caused assertion failures or data corruption in collection handling.

### Before / After
```java
// BEFORE — Purger (CompactionIterator inner class) did not receive nowInSec
private Purger(boolean isForThrift, CompactionController controller) {
    super(isForThrift, controller.gcBefore,
          controller.compactingRepaired() ? Integer.MIN_VALUE : Integer.MAX_VALUE,
          controller.cfs.getCompactionStrategyManager().onlyPurgeRepairedTombstones());
}

// AFTER — nowInSec threaded through
private Purger(boolean isForThrift, CompactionController controller, int nowInSec) {
    super(isForThrift, nowInSec, controller.gcBefore,
          controller.compactingRepaired() ? Integer.MIN_VALUE : Integer.MAX_VALUE,
          controller.cfs.getCompactionStrategyManager().onlyPurgeRepairedTombstones());
}
```

---

## How to Spot
In compaction iterators, always confirm that `nowInSec` is captured at construction time and passed explicitly to every `PurgeFunction` subclass. If the `nowInSec` argument is absent from the constructor chain, the cell-expiry check is using the wrong time reference. Grep for `new Purger(` or `super(isForThrift,` without a `nowInSec` argument.

---

## Test
`TTLExpiryTest.testBug10944` — added to cover the trunk branch as well; validates that collections with map entries survive compaction correctly when `gcBefore` is close to `localDeletionTime`.
