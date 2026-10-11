# Bug 1419: Fix Repeated Log Message When No Hints to Compact

**Commit**: e88bcfeb15 · **Ticket**: CASSANDRA-8247
**Subsystem**: db/HintedHandOffManager
**Category**: Logic error in condition

---

## Root Cause

`HintedHandOffManager.compact()` collected all uncompacting SSTable descriptors and then submitted them to `CompactionManager.submitUserDefined()`. When no hints existed the descriptor list was empty, but the code still called `submitUserDefined()` with an empty set. The compaction manager accepted the call and logged a "no SSTables to compact" message on every invocation — which, depending on scheduling frequency, could spam the logs repeatedly. The fix adds an early-return guard when the descriptor set is empty, skipping the pointless compaction submission entirely.

### Before / After

```java
// BEFORE — empty descriptor list passed to compaction manager, causing repeated log spam
for (SSTable sstable : hintStore.getDataTracker().getUncompactingSSTables())
    descriptors.add(sstable.descriptor);

try {
    CompactionManager.instance.submitUserDefined(hintStore, descriptors, ...).get();
```

```java
// AFTER — early return when there is nothing to compact
for (SSTable sstable : hintStore.getDataTracker().getUncompactingSSTables())
    descriptors.add(sstable.descriptor);

if (descriptors.isEmpty())
    return;

try {
    CompactionManager.instance.submitUserDefined(hintStore, descriptors, ...).get();
```

---

## How to Spot

Whenever a method collects items into a set/list and then passes that collection to a downstream operation, check whether an empty collection is a legal input or a degenerate case that should be short-circuited. Downstream operations often have no good way to distinguish "intentionally empty" from "accidentally empty," so they proceed and may log confusing messages or waste resources. Guard clauses at the collection site prevent this entire class of problem.

---

## Test

No test added.
