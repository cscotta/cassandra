# Bug 0420: `-Xlog:gc` Already-Set Check Uses Anchored Regex, Missing Mid-String Occurrences

**Commit**: 023eecb03a · **Ticket**: CASSANDRA-16279
**Subsystem**: conf (cassandra-env.sh startup script)
**Category**: Logic error in condition

---

## Root Cause

`cassandra-env.sh` uses `grep -q "^-[X]log:gc"` to detect whether a GC logging option has already been added to `JVM_OPTS` before appending the default `-Xlog:gc` flags.  The `^` anchor requires the option to appear at the very start of the `JVM_OPTS` string.  When `-Xlog:gc` is not the first JVM option listed, the pattern fails to match, so the script appends a duplicate GC log flag, causing Java to emit a warning and potentially logging to an unexpected file.  The fix replaces `grep -q "^-[X]log:gc"` with `grep -qe "-[X]log:gc"` (removing the anchor) so any occurrence in the string is detected.

### Before / After

```bash
# BEFORE – only matches if -Xlog:gc is at the very start of JVM_OPTS
echo "$JVM_OPTS" | grep -q "^-[X]log:gc"
```

```bash
# AFTER – matches anywhere in JVM_OPTS
echo "$JVM_OPTS" | grep -qe "-[X]log:gc"
```

---

## How to Spot

Shell scripts that guard against duplicate JVM flags with `grep -q "^-X..."` are fragile: the `^` anchor matches start-of-line, which in a single-line echo only matches if the flag is first.  During review, look for `^` anchors in option-detection greps and ask whether the option could ever appear mid-string.

---

## Test

No automated test added; the fix is verified by manual inspection and the CHANGES.txt entry.
