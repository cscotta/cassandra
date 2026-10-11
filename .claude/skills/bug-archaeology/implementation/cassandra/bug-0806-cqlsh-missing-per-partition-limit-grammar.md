# Bug 0806: CQLSH Autocompletion Missing PER PARTITION LIMIT Clause

**Commit**: 8644aa9b82 · **Ticket**: CASSANDRA-12803
**Subsystem**: CQLSH / CQL grammar
**Category**: Silent skip on non-match

---

## Root Cause
The `SELECT` statement grammar rule in `cql3handling.py` did not include the `PER PARTITION LIMIT` clause, which was added to CQL 3.4 (Cassandra 3.6+). As a result, typing `SELECT ... PER` in cqlsh would produce no autocompletion suggestions and the shell would not recognize the keyword sequence as a valid continuation of the statement, even though the server accepted it.

### Before / After
```python
# BEFORE — PER PARTITION LIMIT missing from SELECT grammar rule
( "ORDER" "BY" <orderByClause> ( "," <orderByClause> )* )?
( "LIMIT" limit=<wholenumber> )?
( "ALLOW" "FILTERING" )?

# AFTER — PER PARTITION LIMIT added before LIMIT
( "ORDER" "BY" <orderByClause> ( "," <orderByClause> )* )?
( "PER" "PARTITION" "LIMIT" perPartitionLimit=<wholenumber> )?
( "LIMIT" limit=<wholenumber> )?
( "ALLOW" "FILTERING" )?
```

---

## How to Spot
When a new CQL syntax feature is implemented server-side, the `cql3handling.py` grammar rules must be updated in lockstep. Review additions to the CQL grammar (`CqlParser.g`) for any new keywords or clauses and verify that a matching rule appears in the Python grammar in `cql3handling.py`. Absence of a clause in the Python grammar silently prevents completion without any error to the user.

---

## Test
No test added.
