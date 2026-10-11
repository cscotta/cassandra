# Bug 0490: cqlsh Uses ensure_str Instead of ensure_text, Breaking Python 3 UTF-8 Input

**Commit**: ffa05ef240 · **Ticket**: CASSANDRA-15739
**Subsystem**: bin/cqlsh.py
**Category**: Wrong serialization / deserialization

---

## Root Cause

In Python 2/3 compatibility mode, `cqlsh.py` defined two helper functions: `ensure_str` (returns a `bytes`/`str` in Py2, `str` in Py3) and `ensure_text` (always returns a unicode `str`). The `get_input_line` and `perform_statement` methods were calling `ensure_str` on user input and statement text, which on Python 3 could produce a bytes object or fail to decode non-ASCII UTF-8 characters properly. The CQL driver expects unicode strings; passing bytes or improperly-typed strings caused encoding errors at runtime.

### Before / After

```python
# BEFORE
def get_input_line(self, prompt=''):
    self.lastcmd = self.stdin.readline()   # may be bytes on Py3
    ...
def perform_statement(self, statement):
    statement = ensure_str(statement)      # wrong helper
```

```python
# AFTER
def get_input_line(self, prompt=''):
    self.lastcmd = ensure_text(self.stdin.readline())  # always unicode
    ...
def perform_statement(self, statement):
    statement = ensure_text(statement)     # correct helper
```

---

## How to Spot

In Py2/Py3 dual-mode code, any site that calls `ensure_str` on data that will be processed as text (not bytes) is a latent encoding bug: on Python 3, `ensure_str` may pass through a bytes object unchanged while `ensure_text` decodes it to unicode. Audit every point where user-supplied text enters the processing pipeline and verify the correct normalization function is applied.

---

## Test

No test added for the encoding fix itself; the commit message notes it also resolves test failures caused by a related cqlsh return-code behavior change.
