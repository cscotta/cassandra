# Bug 1231: TrackerTest Leaves SSTable Deletion Paused on Test Failure

**Commit**: 8b80b55611 · **Ticket**: CASSANDRA-9514
**Subsystem**: test/unit/org/apache/cassandra/db/lifecycle/TrackerTest.java
**Category**: State not cleaned up

---

## Root Cause

`TrackerTest` called `SSTableDeletingTask.pauseDeletions(true)` to prevent background deletion during assertions, but placed the matching `SSTableDeletingTask.pauseDeletions(false)` reset inside the `try` block rather than a `finally` block. If any assertion inside the `try` failed, the pause flag remained set for all subsequent tests in the JVM, causing them to accumulate unreleased SSTable file handles and fail on Linux (where file-descriptor limits are stricter than on macOS). A second problem was that multiple test methods shared the same static `MockSchema.cfs` instance, so SSTable metadata on disk from one test could interfere with the next; the fix adds a `@BeforeClass` cleanup and switches each test to create its own `ColumnFamilyStore` via `MockSchema.newCFS()`.

### Before / After

```java
// BEFORE — pauseDeletions(false) only reached if no assertion throws
try (LifecycleTransaction txn = tracker.tryModify(...)) {
    SSTableDeletingTask.pauseDeletions(true);
    // ... assertions ...
    SSTableDeletingTask.pauseDeletions(false);  // skipped on failure
}
```

```java
// AFTER — guaranteed reset via finally
try {
    SSTableDeletingTask.pauseDeletions(true);
    try (LifecycleTransaction txn = tracker.tryModify(...)) {
        // ... assertions ...
    }
} finally {
    SSTableDeletingTask.pauseDeletions(false);
}
```

---

## How to Spot

Any global or static flag that is toggled before a block of assertions must be reset in a `finally` block, not at the end of the `try` body. Reviewers should scan for `pause`, `disable`, `set`, or similar toggle calls that appear inside `try` without a matching reset in `finally`, especially in test code where assertion failures are expected and normal.

---

## Test

`TrackerTest` itself is the fix: the test now correctly cleans up SSTable deletion state and isolates each test method with its own `ColumnFamilyStore` instance, making the suite reliable on Linux.
