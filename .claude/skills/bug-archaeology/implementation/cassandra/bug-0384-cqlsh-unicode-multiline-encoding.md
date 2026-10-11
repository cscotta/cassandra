# Bug 0384: cqlsh Encoding Error with Unicode in Multi-Line Statement

**Commit**: 288bb9d902 · **Ticket**: CASSANDRA-16539
**Subsystem**: bin/cqlsh.py
**Category**: Wrong serialization / deserialization

---

## Root Cause

When a multi-line CQL statement containing non-ASCII characters is entered in cqlsh, the shell calls `readline.add_history()` to save the statement in the readline history buffer. The statement is assembled by joining lines with spaces, but the resulting Python object could be a `bytes` value (in Python 2) or contain a byte string rather than a Unicode string, causing `readline.add_history()` to raise a `UnicodeDecodeError`. The fix wraps the history entry with `ensure_str()` so that it is always a proper Unicode string before being passed to readline.

### Before / After

```python
# BEFORE
new_hist = srcstr.replace("\n", " ").rstrip()
```

```python
# AFTER
new_hist = ensure_str(srcstr.replace("\n", " ").rstrip())
```

---

## How to Spot

Whenever a shell integration function (readline, logging, socket write) consumes a string derived from user-supplied CQL, check that any bytes-to-str conversion has been applied before the handoff. In Python 2/3 mixed codebases the `ensure_str` / `ensure_bytes` boundary is easily missed when string manipulation (`.replace()`, `.rstrip()`) is chained directly onto raw input; any such chain that ends in a readline or I/O call is a candidate for this class of bug.

---

## Test

`test_multiline_input` in `pylib/cqlshlib/test/test_unicode.py` sends a multi-line INSERT containing a Japanese character over a tty session and asserts the value is readable back via SELECT.
