# Bug 1019: Show CQL help in cqlsh in web browser

**Commit**: 8738087edd · **Ticket**: CASSANDRA-7225
**Subsystem**: CQL Shell (cqlsh)
**Category**: Missing null / bounds check

---

## Root Cause
`cqlsh.py` accessed `webbrowser._tryorder[0]` unconditionally to detect whether `xdg-open` was the first browser candidate. On systems where no browser is registered at all, `webbrowser._tryorder` is an empty list, so the index access raised an `IndexError` and the process crashed before the user could interact with the shell. A second crash path existed later when attempting to open a help URL: if `_tryorder` is empty the code fell through to `webbrowser.open_new_tab()` which also fails when there are no registered browsers, instead of printing a helpful error message.

### Before / After
```python
# BEFORE — unconditional index access crashes when no browser is registered
if webbrowser._tryorder[0] == 'xdg-open' and os.environ.get('XDG_DATA_DIRS', '') == '':
    webbrowser._tryorder.remove('xdg-open')
    webbrowser._tryorder.append('xdg-open')

# AFTER — guard against empty list
if len(webbrowser._tryorder) == 0:
    CASSANDRA_CQL_HTML = CASSANDRA_CQL_HTML_FALLBACK
elif webbrowser._tryorder[0] == 'xdg-open' and os.environ.get('XDG_DATA_DIRS', '') == '':
    webbrowser._tryorder.remove('xdg-open')
    webbrowser._tryorder.append('xdg-open')

# BEFORE — silently tries to open browser even when none is available
if self.browser is not None:
    webbrowser.get(self.browser).open_new_tab(url)
else:
    webbrowser.open_new_tab(url)

# AFTER — prints fallback URL when no browser is present
if len(webbrowser._tryorder) == 0:
    self.printerr("*** No browser to display CQL help. URL for help topic %s : %s" % (t, url))
elif self.browser is not None:
    webbrowser.get(self.browser).open_new_tab(url)
else:
    webbrowser.open_new_tab(url)
```

---

## How to Spot
Any access to a specific index of a Python list that comes from an external/environmental source (here, the browser registry) should be guarded with a length check or a try/except before indexing. In code review, look for `some_list[0]` where `some_list` is not under the caller's control and might be empty. The stale import of `simple_cql_types` from `cql3handling` in `helptopics.py` was also cleaned up in this commit, indicating the file had not been touched since an earlier refactor.

---

## Test
No test added.
