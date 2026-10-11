# Bug 1240: Fix setting the PerfDisableSharedMem JVM option

**Commit**: b540c5d208 · **Ticket**: n/a
**Subsystem**: conf/cassandra-env.sh, conf/cassandra-env.ps1
**Category**: Wrong constant / default value

---

## Root Cause

The JVM flag `-XX:+PerfDisableSharedMem` (enable) was written as `-XX:-PerfDisableSharedMem` (disable) in both the shell and PowerShell startup scripts. In HotSpot JVM flag syntax, `+` enables the flag and `-` disables it; flipping the sign inverts the intended behaviour. The intent was to prevent the JVM from writing performance data to a shared memory file (which can cause I/O pressure on `/tmp` and lock contention), but the misspelled `-` prefix left shared memory enabled.

### Before / After

```sh
# BEFORE
JVM_OPTS="$JVM_OPTS -XX:-PerfDisableSharedMem"
```

```sh
# AFTER
JVM_OPTS="$JVM_OPTS -XX:+PerfDisableSharedMem"
```

---

## How to Spot

Any time a boolean JVM flag is prefixed with `-XX:` followed by `+` or `-`, verify that the sign matches the intent expressed in nearby comments or documentation. A flag named `Disable…` or `Suppress…` that is set with `-` (i.e. "disable the disabler") is an immediate red flag: the double-negative almost always means the sign was accidentally swapped. Code-review checklists for startup configuration files should include a pass where every `-XX:+/-` flag is read aloud as "enable/disable" against the flag's English description.

---

## Test

No test added.
