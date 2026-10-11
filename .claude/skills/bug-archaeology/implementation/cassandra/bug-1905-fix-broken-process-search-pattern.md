# Bug 1905: Broken process-detection pattern in Debian init script

**Commit**: 359d566e81 · **Ticket**: CASSANDRA-6090
**Subsystem**: debian/init
**Category**: Logic error in condition

---

## Root Cause

The Debian init script uses a regex pattern `CMD_PATT` to detect whether the Cassandra process is running by inspecting the process command line. The original pattern `-user.cassandra.+CassandraDaemon` matched on the JVM's `-Duser.name=cassandra` system property, which is part of the command line. That property is not always present or is formatted differently depending on the launch environment, causing `is_running()` to report false negatives (process reported as not running when it is). The fix replaces the pattern with the more stable `cassandra.+CassandraDaemon`, which matches the literal class name path always present in the command.

### Before / After

```bash
# BEFORE
CMD_PATT="-user.cassandra.+CassandraDaemon"
```

```bash
# AFTER
CMD_PATT="cassandra.+CassandraDaemon"
```

---

## How to Spot

When a process-detection regex is anchored to a JVM flag or system property rather than an immutable artifact of the command (such as the main class name), it will break if the flag is removed, renamed, or conditionally set. Prefer patterns that match stable identifiers like the fully-qualified main class name.

---

## Test

No test added.
