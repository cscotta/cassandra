# Bug 0090: AccordCommandsPurger Wrong Durability Check Skips Necessary Cleanup

**Commit**: 1f4675a902 · **Ticket**: n/a
**Subsystem**: db/compaction — CompactionIterator (Accord purger)
**Category**: Logic error in condition

---

## Root Cause

The Accord commands compaction purger used `durableBefore.min(txnId) == Universal` as an early-exit guard to discard partial (sliced) command rows that lacked enough fields to run full cleanup logic. The raw `min()` comparison against the `Universal` durability constant is not a correct safety test; `Cleanup.isSafeToCleanup()` encapsulates the correct multi-field predicate. Similarly, the call to `Commands.shouldCleanup(...)` was invoking the old `Commands` inner-class path instead of the relocated top-level `Cleanup.shouldCleanup(...)`, and was missing the required `false` trailing argument. These errors caused the purger to either skip tombstone emission for universally-durable commands or to invoke the wrong cleanup logic entirely.

### Before / After

```java
// BEFORE
if (durableBefore.min(txnId) == Universal)
    return null;
...
Commands.Cleanup cleanup = Commands.shouldCleanup(txnId, saveStatus.status,
                                                  durability, executeAt, route,
                                                  redundantBefore, durableBefore);
```

```java
// AFTER
if (Cleanup.isSafeToCleanup(durableBefore, txnId))
    return null;
...
Cleanup cleanup = shouldCleanup(txnId, saveStatus.status,
                                durability, executeAt, route,
                                redundantBefore, durableBefore,
                                false);
```

---

## How to Spot

When an API is refactored to move a method or enum from an inner class to a top-level class, all call sites must be updated simultaneously. Any remaining call through the old path (`Commands.Cleanup`, `Commands.shouldCleanup`) will compile if the old symbols still exist as aliases or deprecated overloads, hiding the mismatch. Review imports and static imports after refactors; a stale `import accord.local.Commands` alongside a new `import accord.local.Cleanup` is a strong signal that call sites may be inconsistent.

---

## Test

No test added; the fix is a call-site correction in `CompactionIterator`.
