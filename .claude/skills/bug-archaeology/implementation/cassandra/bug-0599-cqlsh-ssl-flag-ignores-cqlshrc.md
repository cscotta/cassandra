# Bug 0599: cqlsh SSL Flag Ignores cqlshrc Configuration

**Commit**: 845243db93 · **Ticket**: CASSANDRA-14299
**Subsystem**: `bin/cqlsh.py`
**Category**: State not cleaned up

---

## Root Cause

A previous refactor of `read_options()` in `cqlsh.py` reset `optvalues.ssl` to the hard-coded literal `False` after the `cqlshrc` file had already been read into `configs`. This unconditionally overrode any `ssl = true` setting in the `[connection]` section of the user's `cqlshrc`, so SSL could only be enabled via the command-line `--ssl` flag. The fix reads the value from `configs` (with a fallback to `DEFAULT_SSL`) using the same `option_with_default` helper that all other `cqlshrc` options use.

### Before / After

```python
# BEFORE – always False regardless of cqlshrc
optvalues.ssl = False
```

```python
# AFTER – respects cqlshrc [connection] ssl option
optvalues.ssl = option_with_default(configs.getboolean, 'connection', 'ssl', DEFAULT_SSL)
```

---

## How to Spot

In any option-parsing function that reads a config file and then sets defaults, look for hard-coded literal assignments that appear *after* the config file has been parsed — they silently override file-based configuration. A reviewer should verify that every `optvalues.<field> = <literal>` line either appears before the config read or is guarded by a "not already set by config" check.

---

## Test

No test added.
