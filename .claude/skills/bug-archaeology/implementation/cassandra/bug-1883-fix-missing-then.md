# Bug 1883: Missing `then` Keyword in cassandra-env.sh Breaks MALLOC_ARENA_MAX Setting

**Commit**: b0b868f3fc · **Ticket**: n/a
**Subsystem**: conf/cassandra-env.sh
**Category**: Logic error in condition

---

## Root Cause

The shell script `cassandra-env.sh` had an `if` statement with no `then` keyword, making it a syntax error in POSIX `sh` and Bash. Without `then`, the shell would fail to parse the block and the `MALLOC_ARENA_MAX=4` assignment was never executed, leaving the variable unset. This caused Cassandra to start without the memory arena limit, potentially increasing memory fragmentation on glibc systems.

### Before / After

```bash
# BEFORE — missing 'then', shell syntax error
if [ "x$MALLOC_ARENA_MAX" = "x" ]
    MALLOC_ARENA_MAX=4
fi
```

```bash
# AFTER — correct syntax
if [ "x$MALLOC_ARENA_MAX" = "x" ]
then
    MALLOC_ARENA_MAX=4
fi
```

---

## How to Spot

Run `bash -n conf/cassandra-env.sh` (or `sh -n`) as part of CI to syntax-check all shell scripts. Shell linters such as ShellCheck would also flag a missing `then` immediately.

---

## Test

No test added.
