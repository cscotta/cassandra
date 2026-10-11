# Bug 0498: cqlsh Crash on Python 3.7 Missing webbrowser._tryorder

**Commit**: d069fd235f · **Ticket**: CASSANDRA-15572
**Subsystem**: `bin/cqlsh.py`
**Category**: Missing null / bounds check

---

## Root Cause

`cqlsh.py` guards the CQL documentation URL selection with `if len(webbrowser._tryorder) == 0`. In Python 3.7, the `webbrowser` module changed its initialization: `_tryorder` is no longer set to an empty list at import time but is instead left as `None` until browser registration is attempted. Calling `len()` on `None` raises a `TypeError`, causing cqlsh to crash at startup before any user interaction.

### Before / After

```python
# BEFORE — assumes _tryorder is always a list; crashes on Python 3.7 where it is None
if len(webbrowser._tryorder) == 0:
    CASSANDRA_CQL_HTML = CASSANDRA_CQL_HTML_FALLBACK
```

```python
# AFTER — None-guard added before len()
if webbrowser._tryorder is None or len(webbrowser._tryorder) == 0:
    CASSANDRA_CQL_HTML = CASSANDRA_CQL_HTML_FALLBACK
```

---

## How to Spot

When accessing attributes of standard-library modules, check the version history: Python 3.x frequently changes private/semi-private attributes (`_tryorder`, `_browser`, etc.) from initialized empty containers to `None` until lazily populated. Any `len()`, iteration, or index access on such attributes without a prior `None` check is a latent compatibility bug. A simple pre-condition `x is None or len(x) == 0` is idiomatic and version-safe.

---

## Test

No test added.
