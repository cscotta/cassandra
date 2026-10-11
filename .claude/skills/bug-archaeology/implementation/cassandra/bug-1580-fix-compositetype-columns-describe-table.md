# Bug 1580: Fix CompositeType Columns in DESCRIBE TABLE Output

**Commit**: 66e1e46109 · **Ticket**: CASSANDRA-7399
**Subsystem**: bin/cqlsh (Python)
**Category**: Wrong serialization / deserialization

---

## Root Cause

`cqlsh`'s `DESCRIBE TABLE` command rendered `CompositeType` column type annotations using the default `cql_parameterized_type` method, which did not produce a quoted CQL-compatible string for composite types. As a result, the column type in the `DESCRIBE TABLE` output was unquoted and malformed, making it impossible to re-execute the DDL output directly. The fix monkey-patches `CompositeType.cql_parameterized_type` at shell initialisation to produce a properly quoted string.

### Before / After

```python
# BEFORE – no override; CompositeType used the default cql_parameterized_type
#          producing unquoted, non-replayable DDL output
```

```python
# AFTER – override applied at shell init for the 1.2 branch (cql module)
cql.cqltypes.CompositeType.cql_parameterized_type = classmethod(
    lambda cls: "'%s'" % cls.cass_parameterized_type_with(cls.subtypes, True)
)
```

---

## How to Spot

When a type system has a default `cql_parameterized_type` inherited by all subtypes, verify that each subtype that has special serialization requirements (e.g. composite or dynamic composite types) overrides the method. Relying on inheritance for output formatting in schema DDL is risky when subtypes have structurally different representations.

---

## Test

No test added.
