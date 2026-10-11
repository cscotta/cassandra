# Bug 0382: cqlsh DESC TYPE Fails with Non-ASCII Characters in Identifier

**Commit**: 327d7c1803 · **Ticket**: CASSANDRA-16400
**Subsystem**: bin/cqlsh.py
**Category**: Wrong serialization / deserialization

---

## Root Cause

Several `Shell` helper methods used `str(key)` to convert dictionary keys returned by the Cassandra driver (e.g., `keyspaces.keys()`, `tables.keys()`) into Python strings. In Python 3, driver metadata keys may already be strings but `str()` of a non-ASCII string was harmless; however, the `DESCRIBE` output path called `str(row['name'])` on a row value that could carry a non-ASCII byte sequence (a UDT or type name containing accented characters), producing a mangled or exception-raising string instead of the proper Unicode identifier. The fix removes all the spurious `str()` / `map(str, ...)` wrappings and replaces them with a direct list conversion or the existing `ensure_str()` helper that handles encoding correctly.

### Before / After

```python
# BEFORE – wraps driver objects in str(), corrupting non-ASCII identifiers
return list(map(str, list(self.conn.metadata.keyspaces.keys())))

# in DESCRIBE output:
names.append(str(row['name']))
```

```python
# AFTER – iterates directly; uses ensure_str() for output
return list(self.conn.metadata.keyspaces)

names = [ensure_str(r['name']) for r in rows]
```

---

## How to Spot

Any place that wraps a Python 3 string-like object in `str()` to "make it a string" is suspicious: in Python 3, strings are already Unicode, and calling `str()` on driver objects that implement `__str__` may invoke an incorrect encoding path. Look for `map(str, …)` patterns on iterable metadata collections in cqlsh.

---

## Test

`test_unicode.py` was extended with a test case that describes a UDT whose name contains non-ASCII characters and verifies the output is correctly encoded.
