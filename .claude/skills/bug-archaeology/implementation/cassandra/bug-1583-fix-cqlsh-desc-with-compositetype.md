# Bug 1583: Fix cqlsh DESC with CompositeType Explanation

**Commit**: 1e6493083f · **Ticket**: CASSANDRA-7399
**Subsystem**: bin/cqlsh (Python)
**Category**: Wrong serialization / deserialization

---

## Root Cause

`cqlsh`'s `DESC` (DESCRIBE) command rendered `CompositeType` and `DynamicCompositeType` columns using the default `cql_parameterized_type` method, which produced unquoted, non-replayable DDL output. This is the trunk/2.1 branch fix for the same defect addressed in bug 1580 (commit 66e1e46109, branch 1.2): both `CompositeType` and `DynamicCompositeType` needed their `cql_parameterized_type` class method patched at shell startup to emit a properly quoted string.

### Before / After

```python
# BEFORE – no override; both CompositeType and DynamicCompositeType
#          fell through to the default, producing malformed DDL
```

```python
# AFTER – both types patched at shell init (trunk uses cassandra.cqltypes module)
type_for_composites = lambda cls: "'%s'" % cls.cass_parameterized_type_with(cls.subtypes, True)
cassandra.cqltypes.CompositeType.cql_parameterized_type = classmethod(type_for_composites)
cassandra.cqltypes.DynamicCompositeType.cql_parameterized_type = classmethod(type_for_composites)
```

---

## How to Spot

When applying a fix to one branch that patches runtime behaviour via monkey-patching, verify that all related types (not just the one surfacing in the bug report) need the same patch. `DynamicCompositeType` had the same defect and was added in this follow-on fix.

---

## Test

No test added.
