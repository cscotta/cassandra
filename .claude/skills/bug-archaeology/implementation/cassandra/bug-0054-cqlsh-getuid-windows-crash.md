# Bug 0054: CQLSH Crashes Loading .cqlshrc on Windows Due to Missing os.getuid()

**Commit**: 49c8122d8f · **Ticket**: CASSANDRA-20478
**Subsystem**: pylib/cqlshlib/util.py
**Category**: Missing null / bounds check

---

## Root Cause

`is_file_secure` called `os.getuid()` after `os.stat()` succeeded, but only caught `OSError` from `stat`. On Windows, `os.getuid()` does not exist and raises `AttributeError`. Because `AttributeError` was not caught, loading `.cqlshrc` on any non-Unix platform threw an unhandled exception at CQLSH startup, preventing users from connecting even when the server itself ran on a supported platform.

### Before / After

```python
# BEFORE – AttributeError from os.getuid() on Windows was uncaught
try:
    st = os.stat(filename)
except OSError as e:
    if e.errno != errno.ENOENT:
        raise
    return True
uid = os.getuid()   # raises AttributeError on Windows
```

```python
# AFTER – AttributeError caught and treated as "not Unix, skip security check"
try:
    st = os.stat(filename)
    uid = os.getuid()
except OSError as e:
    if e.errno != errno.ENOENT:
        raise
    return True
except AttributeError:
    # not-Unix os
    return True
```

---

## How to Spot

When calling POSIX-only APIs like `os.getuid()`, `os.getgid()`, or `os.lstat()`, verify that every call site catches `AttributeError` (or guards with `hasattr(os, 'getuid')`) to avoid crashes on non-Unix platforms. In security-check helpers that are expected to return a boolean, an uncaught platform-compatibility exception is especially harmful because it converts a safe "skip the check" path into a crash.

---

## Test

No automated test added; the fix was evaluated by manual verification on non-Unix platforms.
