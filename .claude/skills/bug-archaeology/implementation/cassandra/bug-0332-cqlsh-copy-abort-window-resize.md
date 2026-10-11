# Bug 0332: cqlsh COPY Aborts on Terminal Window Resize

**Commit**: 75e0e8cf41 · **Ticket**: CASSANDRA-15230
**Subsystem**: pylib/cqlshlib/copyutil.py
**Category**: Logic error in condition

---

## Root Cause

`ReceivingChannels._recv_linux` called Python's `select.select()` to wait for data on inter-process pipes. When the terminal window is resized the OS sends `SIGWINCH` to the process, which interrupts the `select()` system call and raises `select.error` with `errno.EINTR`. The original code imported `select` directly (`from select import select`) and caught no exceptions around the call, so `EINTR` propagated up as an unhandled error and aborted the entire COPY operation.

### Before / After

```python
# BEFORE – no EINTR handling; any signal during select() aborts COPY
from select import select
readable, _, _ = select(self._readers, [], [], timeout)
```

```python
# AFTER – retry on EINTR, raise on any other error
import select, errno
while True:
    try:
        readable, _, _ = select.select(self._readers, [], [], timeout)
    except select.error, exc:
        if exc[0] != errno.EINTR:
            raise
    else:
        break
```

---

## How to Spot

Any blocking system call (`select`, `poll`, `read`, `write`, `accept`) can be interrupted by a signal and return `EINTR` / raise the equivalent exception. Code that wraps such calls without checking for `EINTR` and retrying will fail spuriously on any signal delivery, including the innocuous `SIGWINCH`. During review, look for unguarded `select`/`poll` calls in Python 2 code (Python 3 restarts them automatically for most signals, but Python 2 does not).

---

## Test

No automated test added; the fix is verified manually by resizing the terminal window while a `COPY` import/export is in progress.
