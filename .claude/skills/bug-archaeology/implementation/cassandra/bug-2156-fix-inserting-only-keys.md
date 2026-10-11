# Bug 2156: CQL3 INSERT Rejected When Only a Partition Key Is Provided

**Commit**: d6ccfb53cf · **Ticket**: CASSANDRA-5040
**Subsystem**: src/java/org/apache/cassandra/cql3/Cql.g
**Category**: Logic error in condition

---

## Root Cause

The CQL3 grammar for `INSERT` required at least two column names and two values (`+` quantifier), preventing valid inserts that specify only the partition key with no additional columns. The `+` quantifier in the ANTLR grammar mandated one-or-more repetitions of the comma-separated column and value lists beyond the first element, making single-column inserts a parse error. Changing to `*` (zero-or-more) allows a single column/value pair.

### Before / After

```antlr
// BEFORE — requires two or more columns/values
'(' c1=cident { columnNames.add(c1); }  ( ',' cn=cident { columnNames.add(cn); } )+ ')'
K_VALUES
'(' v1=set_operation { columnOperations.add(v1); } ( ',' vn=set_operation { columnOperations.add(vn); } )+ ')'
```

```antlr
// AFTER — allows a single column/value pair
'(' c1=cident { columnNames.add(c1); }  ( ',' cn=cident { columnNames.add(cn); } )* ')'
K_VALUES
'(' v1=set_operation { columnOperations.add(v1); } ( ',' vn=set_operation { columnOperations.add(vn); } )* ')'
```

---

## How to Spot

In grammar rules for list constructs, `+` means "one or more additional items" on top of a mandatory first item, which effectively enforces a minimum of two. Use `*` when the intent is "one or more total." Verify grammar quantifiers against documented CQL semantics whenever adding or modifying statement rules.

---

## Test

No test added.
