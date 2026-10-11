# Bug 1882: nodetool status Crashes with ArrayIndexOutOfBoundsException When No Keyspace Argument

**Commit**: 8d319b06a1 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/tools/NodeCmd.java
**Category**: Missing null / bounds check

---

## Root Cause

The `STATUS` command handler in `NodeCmd` unconditionally accessed `arguments[0]` to pass an optional keyspace filter to `printClusterStatus()`. When `nodetool status` was invoked without a keyspace argument `arguments` was an empty array, causing an `ArrayIndexOutOfBoundsException`. The fix checks `arguments.length > 0` before dereferencing the first element, passing `null` to indicate "all keyspaces" when no argument is provided.

### Before / After

```java
// BEFORE — unconditional array access crashes on zero arguments
case STATUS :
    nodeCmd.printClusterStatus(System.out, arguments[0]);
    break;
```

```java
// AFTER — bounds-checked, passes null when no keyspace is specified
case STATUS :
    if (arguments.length > 0) nodeCmd.printClusterStatus(System.out, arguments[0]);
    else                      nodeCmd.printClusterStatus(System.out, null);
    break;
```

---

## How to Spot

Any command-line argument handler that accesses a positional argument without first checking the array length is at risk. Review every `arguments[N]` access and confirm the surrounding code verifies `arguments.length > N` before the access. A simple integration test that runs each nodetool command with its minimum required arguments (and without optional ones) would have caught this.

---

## Test

No test added.
