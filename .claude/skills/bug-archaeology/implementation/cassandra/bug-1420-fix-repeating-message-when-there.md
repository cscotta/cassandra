# Bug 1420: Fix Repeating Log Message When No Hints to Compact (Branch Backport)

**Commit**: 827d93e22f · **Ticket**: CASSANDRA-8247
**Subsystem**: db/HintedHandOffManager
**Category**: Logic error in condition

---

## Root Cause

This is a parallel backport of the same fix applied in commit e88bcfeb15 (bug 1419) to a different branch of the codebase. The root cause is identical: `HintedHandOffManager.compact()` collected SSTable descriptors and then unconditionally called `CompactionManager.submitUserDefined()` even when the descriptor list was empty, causing the compaction manager to emit a repeated "nothing to compact" log message on every hint-compaction scheduling cycle. The fix inserts the same `if (descriptors.isEmpty()) return;` guard before the submission.

### Before / After

```java
// BEFORE — unconditional submission with potentially empty list
for (SSTable sstable : hintStore.getDataTracker().getUncompactingSSTables())
    descriptors.add(sstable.descriptor);

try {
    CompactionManager.instance.submitUserDefined(hintStore, descriptors, ...).get();
```

```java
// AFTER — bail out early if there is nothing to submit
for (SSTable sstable : hintStore.getDataTracker().getUncompactingSSTables())
    descriptors.add(sstable.descriptor);

if (descriptors.isEmpty())
    return;

try {
    CompactionManager.instance.submitUserDefined(hintStore, descriptors, ...).get();
```

---

## How to Spot

See bug 1419 — the heuristic is the same. When a fix is applied to one branch and a separate commit applies it to another branch, treat both commits as structurally equivalent. Reviewers should confirm that the context lines (line numbers, surrounding code) align with the expected branch state and that no additional side effects were introduced during the port.

---

## Test

No test added.
