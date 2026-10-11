# Bug 0367: Cassandra Startup Scripts Silently Use Wrong Java When JAVA_HOME Unset

**Commit**: 75ddf76801 · **Ticket**: CASSANDRA-14325
**Subsystem**: bin/cassandra, bin/debug-cql
**Category**: Wrong constant / default value

---

## Root Cause

When `JAVA_HOME` was not set, both `bin/cassandra` and `bin/debug-cql` set `JAVA=java`. The subsequent guard `if [ -z $JAVA ]` never triggers because the literal string `java` is non-empty, so the scripts proceed even when `java` is not present anywhere on `PATH`. At execution time the shell then raises a "command not found" error with no helpful diagnostic. Using `command -v java 2>/dev/null` instead evaluates immediately to the resolved path (or empty string if not found), making the existing "java not found" error check work correctly and giving a clear failure message before attempting to start the JVM.

### Before / After

```sh
# BEFORE — always non-empty; guard never fires even if java is absent from PATH
JAVA=java
```

```sh
# AFTER — resolves to absolute path or empty string
JAVA=`command -v java 2> /dev/null`
```

---

## How to Spot

In shell scripts, assigning a bare command name to a variable and then checking for emptiness is a well-known anti-pattern: the variable will always contain the literal string rather than the resolved binary. Review every `JAVA=java` or similar assignment followed by a `[ -z $VAR ]` guard; the guard should use `command -v` or `which` to probe existence before the assignment.

---

## Test

No test added.
