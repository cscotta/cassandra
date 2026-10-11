# Bug 0714: cqlsh COPY FROM crashes with "unhashable type: list" on list columns

**Commit**: 010b5f3a56 · **Ticket**: CASSANDRA-13364
**Subsystem**: cqlsh / COPY FROM
**Category**: Wrong serialization / deserialization

---

## Root Cause
In `ImportConversion`, the `convert_list` helper returned a plain Python `list` object. A later step placed the converted collection value into a Python `set` (via a hash-based data structure) as part of deduplication or batching logic, which raised `TypeError: unhashable type: list`. The fix changes `convert_list` to return a `tuple`, which is hashable, consistent with how `convert_tuple` already worked and how `convert_set` returns a `frozenset`.

### Before / After
```python
# BEFORE
def convert_list(val, ct=cql_type):
    return list(convert_mandatory(ct.subtypes[0], v) for v in split(val))

# AFTER
def convert_list(val, ct=cql_type):
    return tuple(convert_mandatory(ct.subtypes[0], v) for v in split(val))
```

---

## How to Spot
When an intermediate Python representation of a CQL collection is used in a context that requires hashability (dict key, set element), mutable container types such as `list` will raise `TypeError` at runtime. Review all `convert_*` helpers to ensure their return types are either immutable (`tuple`, `frozenset`) or are never placed into hash-based containers downstream.

---

## Test
No dedicated test added in this commit.
