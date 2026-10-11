# Bug 1366: Prevent CommitLogTest failures from cascading

**Commit**: 5e797ebbad · **Ticket**: CASSANDRA-7713
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/CommitLog.java
**Category**: State not cleaned up

---

## Root Cause

`CommitLog.handleCommitError` had package-private visibility, which prevented test code from calling it directly to simulate commit failures. The test `testCommitFailurePolicy_stop` worked around this by making the commit log directory non-writable and triggering a real flush — a fragile approach that left the directory in a read-only state if the test failed mid-execution, causing all subsequent tests in the same JVM to fail with I/O errors. The underlying production issue was that no clean way existed to inject a commit failure without side effects on the filesystem, so the test was commented out entirely, leaving the `CommitFailurePolicy.stop` path untested.

### Before / After

```java
// BEFORE — package-private, not accessible from test; test resorted to filesystem manipulation
static boolean handleCommitError(String message, Throwable t)
```

```java
// AFTER — public + @VisibleForTesting; test calls it directly without touching the filesystem
@VisibleForTesting
public static boolean handleCommitError(String message, Throwable t)
```

---

## How to Spot

Tests that manipulate global filesystem state (chmod, deleting directories) to force error paths are a red flag: they rely on OS-level side effects that are not reset atomically if the test aborts. Whenever a production method that models a failure mode is not accessible for direct injection, the right fix is to widen visibility with `@VisibleForTesting` rather than simulate the failure indirectly through environmental mutation. Review tests for `setWritable(false)`, `File.delete()`, or other environmental side effects inside `try` blocks with fragile cleanup.

---

## Test

The existing `testCommitFailurePolicy_stop` test was rewritten to call `CommitLog.handleCommitError` directly and assert that `Gossiper.instance.isEnabled()` becomes false, removing all filesystem manipulation.
