# Bug 0213: BinPackRewriteFilePlanner computes output file count from full group size instead of truncated subset

**Commit**: 06f9a1d1d383fc29c82dfba099e78a65a6a3188a · **Ticket**: #15576

## Detection Rule

When a list of items is truncated to a subset before being processed, verify that any size-derived metrics (expected counts, split sizes) are computed from the subset's size, not from the original full list's size.

## Code Smell

A call to `expectedOutputFiles(inputSize)` and `inputSplitSize(inputSize)` where `inputSize` was computed from the original `fileScanTasks` list, but only `fileScanTasks.subList(0, scanTasksToRewrite)` is actually passed to the rewrite group.

## Tags

- scope: universal
- category: logic-error
- code-pattern: stale-size-after-subset
- severity: silent-wrong-result
- subsystem: core/actions/BinPackRewriteFilePlanner

## Root Cause

When `max-files-to-rewrite` limits the number of files in a group, the code correctly sliced `fileScanTasks` to `scanTasksToRewrite` elements, but continued to pass `inputSize` (the size of the full group) to `expectedOutputFiles` and `inputSplitSize`. This caused the planner to schedule too many output files for the truncated group, wasting resources and producing incorrect compaction plans.

## Example

```java
// BEFORE
int scanTasksToRewrite = Math.min(fileScanTasks.size(), remainingSize);
selectedFileGroups.add(
    newRewriteGroup(
        ctx,
        partition,
        fileScanTasks.subList(0, scanTasksToRewrite),
        inputSplitSize(inputSize),       // inputSize = full group
        expectedOutputFiles(inputSize))); // inputSize = full group
```

```java
// AFTER
int scanTasksToRewrite = Math.min(fileScanTasks.size(), remainingSize);
List<FileScanTask> tasksToRewrite = fileScanTasks.subList(0, scanTasksToRewrite);
long rewriteInputSize = inputSize(tasksToRewrite);
selectedFileGroups.add(
    newRewriteGroup(
        ctx,
        partition,
        tasksToRewrite,
        inputSplitSize(rewriteInputSize),      // computed from subset
        expectedOutputFiles(rewriteInputSize))); // computed from subset
```
