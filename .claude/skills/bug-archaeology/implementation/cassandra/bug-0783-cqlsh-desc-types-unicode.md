# Bug 0783: cqlsh DESC TYPES fails on identifiers containing unicode

**Commit**: d38bf9faa4 · **Ticket**: CASSANDRA-12914
**Subsystem**: CQL Shell (cqlsh)
**Category**: Wrong serialization / deserialization

---

## Root Cause
`describe_functions`, `describe_aggregates`, and `describe_user_types` all called `cmd.Cmd.columnize()` with the result of `protect_names(ksmeta.<collection>.keys())`. Under Python 2, `protect_names` returns byte strings, but the keyspace/type-name keys are unicode strings; passing a mixed-encoding list to `cmd.Cmd.columnize()` caused a `UnicodeEncodeError` whenever any identifier contained non-ASCII characters. The fix introduces a dedicated helper `_columnize_unicode` that first encodes names to UTF-8 bytes before columnizing, and for user types additionally applies `protect_names` to the already-encoded byte strings so that quoting is applied correctly.

### Before / After
```python
# BEFORE — describe_user_types (same pattern in describe_functions / describe_aggregates)
cmd.Cmd.columnize(self, protect_names(ksmeta.user_types.keys()))
print

# AFTER
self._columnize_unicode(ksmeta.user_types.keys(), quote=True)

# NEW HELPER
def _columnize_unicode(self, name_list, quote=False):
    """
    Used when columnizing identifiers that may contain unicode
    """
    names = [n.encode('utf-8') for n in name_list]
    if quote:
        names = protect_names(names)
    cmd.Cmd.columnize(self, names)
    print
```

---

## How to Spot
In Python 2 cqlsh code, any call to `cmd.Cmd.columnize()` with a list derived from schema-metadata dictionary keys is a candidate for this bug, because schema object names are unicode strings that can contain non-ASCII characters. Review every `columnize` call site to confirm that identifiers are encoded to bytes before being passed in, and that `protect_names` (which adds quoting) is applied after encoding, not before.

---

## Test
No test added.
