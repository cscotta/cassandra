# Bug 0346: cqlsh Shell Script Version Check Broken on Python 3.10+

**Commit**: 5f97977dfa · **Ticket**: CASSANDRA-16987
**Subsystem**: bin/cqlsh
**Category**: Logic error in condition

---

## Root Cause

The `is_supported_version` function used a shell string comparison (`[ ! "$version" \< "3.6" ]`) to determine whether the Python version was supported. Shell lexicographic ordering treats "3.10" as less than "3.6" because "1" sorts before "6" character-by-character. When Python 3.10 was released, the version string "3.10" compared as less than "3.6" under this scheme, causing cqlsh to report Python 3.10 as unsupported and refuse to start.

### Before / After

```bash
# BEFORE
# shellcheck disable=SC2072
if [ ! "$version" \< "3.6" ] || [ "$version" = "2.7" ]; then
```

```bash
# AFTER
major_version="${version%.*}"
minor_version="${version#*.}"
if [ "$major_version" = 3 ] && [ "$minor_version" -ge 6 ] || [ "$version" = "2.7" ]; then
```

---

## How to Spot

Shell scripts that version-compare dotted version strings with `<` or `>` string operators are always fragile for multi-digit minor versions. Whenever a script does version gating, split the string into numeric major/minor components and use arithmetic (`-ge`, `-lt`) comparisons rather than lexicographic string comparisons.

---

## Test

No test added.
