# Bug 0209: CQLSH Fallback Help URL Points to Removed CQL 3.0 Page

**Commit**: 28c04fbdde · **Ticket**: CASSANDRA-17534
**Subsystem**: bin/cqlsh.py
**Category**: Wrong constant / default value

---

## Root Cause

The `CASSANDRA_CQL_HTML_FALLBACK` constant in `cqlsh.py` was hard-coded to
`https://cassandra.apache.org/doc/cql3/CQL-3.0.html`, a URL that was removed
when the documentation site was reorganised.  Whenever no local `CQL.html` file
was found, the `HELP TOPICS` command (and online help navigation) directed users
to a dead link.  The fix replaces it with the current canonical URL at
`https://cassandra.apache.org/doc/latest/cassandra/cql/cql_singlefile.html`.

### Before / After

```python
# BEFORE
CASSANDRA_CQL_HTML_FALLBACK = 'https://cassandra.apache.org/doc/cql3/CQL-3.0.html'
```

```python
# AFTER
CASSANDRA_CQL_HTML_FALLBACK = 'https://cassandra.apache.org/doc/latest/cassandra/cql/cql_singlefile.html'
```

---

## How to Spot

Whenever a URL or file path is stored as a string constant, verify that it still
resolves during review — especially after site migrations or restructuring.
Fallback / "offline not found" URLs are easy to overlook in testing because the
happy path (local file exists) is exercised far more often.

---

## Test

No test added.
