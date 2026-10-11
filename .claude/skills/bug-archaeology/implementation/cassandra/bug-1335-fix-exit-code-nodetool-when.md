# Bug 1335: Nodetool ring Returns Exit Code 0 When Keyspace Does Not Exist

**Commit**: 4223aad668 · **Ticket**: CASSANDRA-8776
**Subsystem**: src/java/org/apache/cassandra/tools/NodeTool.java
**Category**: Wrong constant / default value

---

## Root Cause

When `nodetool ring` was called with a non-existent keyspace, the `probe.effectiveOwnership()` call threw an `IllegalArgumentException`. The catch block printed the error message to stdout and then issued a bare `return`, which allowed the JVM to exit with code 0 — indicating success to any calling script or CI system. The fix replaces `return` with `System.exit(1)` so that a non-existent keyspace produces a non-zero exit code. A secondary issue was that `ownerships` was declared without initialization; if any later code path had used it without the exception being caught, it would have been uninitialized. Initializing it to `null` also fixes a potential compile-time-uninitialized-variable warning.

### Before / After

```java
// BEFORE
catch (IllegalArgumentException ex) {
    System.out.printf("%nError: " + ex.getMessage() + "%n");
    return;  // exits with code 0 — caller cannot detect failure
}
```

```java
// AFTER
catch (IllegalArgumentException ex) {
    System.out.printf("%nError: " + ex.getMessage() + "%n");
    System.exit(1);  // signals failure to the caller
}
```

---

## How to Spot

CLI tools that catch error conditions and silently return without setting a non-zero exit code will mislead scripts and automation that check `$?`. During review, any `catch` block in a command-line entry point that prints an error and then returns (rather than exiting with a failure code) should be flagged. The convention for CLI tools is that any user-visible error message must be accompanied by a non-zero exit.

---

## Test

No test added.
