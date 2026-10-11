# Bug 0119: cqlsh Autocompletion for Role and User Names Accesses Wrong Row Field

**Commit**: 4f9e474bb1 · **Ticket**: CASSANDRA-20175
**Subsystem**: `pylib/cqlshlib/cql3handling.py`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The `rolename_completer` function retrieved role names from `LIST ROLES` using positional index `row[0]`, and `username_name_completer` retrieved user names from `LIST USERS` using `list(row.values())[0]`. Both approaches are fragile: `row[0]` relies on undocumented column ordering in the result set, and `list(row.values())[0]` relies on dict insertion order. When the result set columns were reordered (or a different driver version was used), these expressions would return the wrong field—for example, an internal UUID or a boolean flag instead of the actual name—producing garbage completions. Additionally, the `<rolename>` grammar rule lacked a named capture group, so the completer was never triggered. The fix adds the `role=( ... )` named capture group to the grammar rule and replaces the positional field accesses with explicit named field lookups (`row['role']` and `row['name']`).

### Before / After

```python
# BEFORE – positional access, breaks if column order changes
return [maybe_quote(row[0].replace("'", "''")) for row in session.execute("LIST ROLES")]
```

```python
# AFTER – named field access, resilient to column reordering
return map(maybe_escape_name, [row['role'] for row in session.execute("LIST ROLES")])
```

---

## How to Spot

When iterating over database result rows, always access columns by name rather than by position or by iterating values(). Positional access and values() iteration both silently break if the server adds, removes, or reorders columns. The pattern `list(row.values())[0]` is especially dangerous because it converts an ordered dict to a list just to take the first element, coupling correctness to a hidden implementation detail of the driver's row representation.

---

## Test

`TestCqlshCompletion#test_complete_in_create_role` asserts that `CREATE ROLE ` and `CREATE USER ` completions return the expected hint tokens rather than corrupted role data.
