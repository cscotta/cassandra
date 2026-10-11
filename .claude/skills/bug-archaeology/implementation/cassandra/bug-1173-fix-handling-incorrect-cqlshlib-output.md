# Bug 1173: cqlsh Timestamp Formatter Produces Garbage Timezone Output on Windows

**Commit**: 99decd8efa · **Ticket**: CASSANDRA-9418
**Subsystem**: pylib/cqlshlib/formatting.py
**Category**: Logic error in condition

---

## Root Cause

The `strftime` helper in `formatting.py` contained a workaround for the fact that Python's `%z` format specifier is not supported on all platforms (CASSANDRA-4746). The workaround checked whether the last four characters of the formatted string were `'0000'` and whether the format ended with `%z`, and if so replaced the tail with a proper `+HHMM` offset. On Windows, however, the C runtime expands `%z` not to a numeric offset but to a long English string such as `'Eastern Standard Time'`, making the four-character tail check meaningless. The fix detects Windows at module load time (`is_win`) and takes a separate branch that strips all alphabetic characters from the formatted string with a regex before appending the computed `+HHMM` suffix. A secondary bug in `cqlsh.py` was also fixed: the readline-missing warning used `platform.system()` directly rather than the pre-computed `myplatform` variable, which was always populated correctly elsewhere.

### Before / After

```python
# BEFORE: tail check breaks on Windows where %z expands to long text
if formatted[-4:] != '0000' or time_format[-2:] != '%z' or offset == 0:
    return formatted
# ...
return formatted[:-5] + sign + '{0:0=2}{1:0=2}'.format(hours, minutes)
```

```python
# AFTER: Windows detected at import time; alphabetics stripped before appending offset
is_win = platform.system() == 'Windows'
# ...
if not is_win and (formatted[-4:] != '0000' or time_format[-2:] != '%z' or offset == 0):
    return formatted
elif is_win and time_format[-2:] != '%z':
    return formatted
# Windows branch:
if is_win and time_format[-2:] == '%z':
    formatted = re.sub('[A-Za-z]', '', formatted).rstrip()
    return formatted + sign + '{0:0=2}{1:0=2}'.format(hours, minutes)
```

---

## How to Spot

Platform-specific string format workarounds are fragile whenever the assumption about the "bad" output is hard-coded. A reviewer should ask: "What does the platform actually emit?" rather than accepting a tail-string check at face value. Whenever `platform.system()` is used to branch on OS but the actual output of a system call is not verified empirically on that OS, the branch condition is speculative and liable to be wrong.

---

## Test

No test added.
