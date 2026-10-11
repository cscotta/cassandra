# Bug 0378: cqlsh In-Browser Help Broken Under Python 3

**Commit**: 9783d47b9f · **Ticket**: CASSANDRA-16658
**Subsystem**: bin/cqlsh.py
**Category**: Logic error in condition

---

## Root Cause

cqlsh checked `webbrowser._tryorder is None or len(webbrowser._tryorder) == 0` to decide whether a browser was available. In Python 3.7+, `webbrowser._tryorder` is `None` until `register_standard_browsers()` is explicitly called (lazy initialization), so the check always evaluated to true and the help command immediately fell back instead of opening a browser. Additionally, the fallback HTML URL pointed to the long-removed CQL 3.2 page. The fix calls `webbrowser.register_standard_browsers()` eagerly at import time and checks the return value of `open_new_tab` to decide whether to print the fallback URL.

### Before / After

```python
# BEFORE – webbrowser._tryorder is None in Python 3.7+, so browser is never used
if webbrowser._tryorder is None or len(webbrowser._tryorder) == 0:
    CASSANDRA_CQL_HTML = CASSANDRA_CQL_HTML_FALLBACK
elif webbrowser._tryorder[0] == 'xdg-open' and ...:
    ...
```

```python
# AFTER – eagerly register browsers, then check open result
try:
    webbrowser.register_standard_browsers()
except AttributeError:
    pass
if webbrowser._tryorder and webbrowser._tryorder[0] == 'xdg-open' and ...:
    ...
# later, in help:
opened = webbrowser.open_new_tab(url)
if not opened:
    self.printerr("*** No browser ...")
```

---

## How to Spot

When code inspects private/internal attributes of a standard library module (`webbrowser._tryorder`) to determine capability, verify the behavior of those attributes across Python versions. Lazy-initialized private state is a portability trap: behavior that works in Python 2 or early Python 3 may silently regress when the initialization contract changes in a later release.

---

## Test

No test added.
