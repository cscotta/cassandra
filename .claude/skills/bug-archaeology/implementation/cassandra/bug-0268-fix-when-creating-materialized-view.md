# Bug 0268: cqlsh `WITH` Keyword Not Suggested After `PRIMARY KEY` in `CREATE MATERIALIZED VIEW`

**Commit**: 83c1f7713b · **Ticket**: CASSANDRA-17879
**Subsystem**: pylib/cqlshlib/cql3handling.py
**Category**: Silent skip on non-match

---

## Root Cause

The `createMaterializedViewStatement` grammar rule in cqlsh's completion engine did not encode the full `CREATE MATERIALIZED VIEW ... PRIMARY KEY ... WITH` syntax. The rule stopped after `PRIMARY KEY <pkDef>` with no `WITH` clause production, so after the user typed the primary key definition the completer had no continuations to offer. Additionally, `working_on_keyspace` called `ctxt.get_binding('wat')` without a default, raising a `KeyError` when `wat` was absent—for example during `CREATE MATERIALIZED VIEW` before the keyword was bound—which silently suppressed all property completions. A new `elif` branch in `prop_name_completer` also filters `default_time_to_live` and `gc_grace_seconds`, which are not valid materialized view properties.

### Before / After

```python
# BEFORE – grammar stops at PRIMARY KEY, no WITH production; get_binding with no default
def working_on_keyspace(ctxt):
    wat = ctxt.get_binding('wat').upper()
    ...

<createMaterializedViewStatement> ::= "CREATE" "MATERIALIZED" "VIEW" ...
                                      "PRIMARY" "KEY" <pkDef>
                                    ;
```

```python
# AFTER – grammar includes WITH clause; get_binding has safe default
def working_on_keyspace(ctxt):
    wat = ctxt.get_binding('wat', '').upper()
    ...

<createMaterializedViewStatement> ::= "CREATE" wat="MATERIALIZED" "VIEW" ...
                                      "PRIMARY" "KEY" (...)
                                      ( "WITH" <cfamProperty> ( "AND" <cfamProperty> )* )?
                                    ;
```

---

## How to Spot

Grammar-driven completion rules must mirror the actual CQL grammar completely; any missing optional clause at the end of a production silently truncates the completion token stream. `ctxt.get_binding` calls without a default argument are fragile: if the binding is absent (common for early-typed tokens) they raise a `KeyError`, which completion handlers typically swallow, producing an empty suggestion list.

---

## Test

`TestCqlshCompletion.test_complete_in_create_materializedview` verifies that `WITH` and `;` are offered as completions after the `PRIMARY KEY` clause.
