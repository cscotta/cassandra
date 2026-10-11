**Commit**: 8125c3da5b · **Ticket**: KAFKA-17703
**Subsystem**: coordinator/share (DelayedShareFetch)
**Category**: Logic error in condition

**Root cause**
`DelayedShareFetch` held direct references to `ActionQueue` and `delayedShareFetchPurgatory`, creating tight coupling that required both to be passed at construction time. This made the purgatory reference unavoidable even in unit tests and forced callers to know about internal scheduling details. The refactor moves purgatory/action-queue responsibility into `SharePartitionManager` (accessed via a single reference), introduces dedicated key types, and adds a manager-level helper method for triggering completion, decoupling the delayed operation from its infrastructure.

**Before/After**
```java
// Before – purgatory injected into delayed op
new DelayedShareFetch(req, partitionMap, actionQueue, purgatory);

// After – only manager reference needed
new DelayedShareFetch(req, partitionMap, sharePartitionManager);
// manager exposes:
sharePartitionManager.addPurgatoryCheckAndCompleteDelayedActionToActionQueue(key);
```

**How to spot**
If a delayed purgatory operation holds a reference to its own purgatory, it can trigger its own completion — a circular dependency that makes testing hard and creates subtle ordering bugs; ownership of purgatory checks should belong to the manager, not the delayed op.

**Test**
Unit tests refactored in `DelayedShareFetchTest`.
