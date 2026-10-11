# Bug 1633: Fix C* launch issues on Russian OS locale

**Commit**: 16fd1a4a89 · **Ticket**: CASSANDRA-6162
**Subsystem**: conf/cassandra-env.sh
**Category**: Logic error in condition

---

## Root Cause

The heap-sizing script used `awk '/Mem:/ {print $2}'` to parse the output of `free -m` and extract available system memory. On Linux systems running a Russian locale, the `free` command translates its output headers — "Mem:" becomes a Cyrillic equivalent — so the `/Mem:/` pattern never matches and the variable is left empty. The script then fails to calculate heap sizes, preventing Cassandra from starting. Replacing the pattern with `/:/ {print $2; exit}` matches the first line containing a colon regardless of locale, making the parsing locale-independent.

### Before / After

```bash
# BEFORE
system_memory_in_mb=`free -m | awk '/Mem:/ {print $2}'`
```

```bash
# AFTER
system_memory_in_mb=`free -m | awk '/:/ {print $2;exit}'`
```

---

## How to Spot

When parsing command-line tool output in shell scripts, verify whether the tool honours the system locale and translates its labels. Patterns that match English-only strings will silently produce empty output on non-English systems; prefer matching on structural markers (punctuation, column position) rather than translated words.

---

## Test

No test added.
