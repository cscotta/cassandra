# Bug 2485: cqlsh ASSUME decoder uses current keyspace, ignores keyspace.cfname qualifier

**Commit**: b23a805925 · **Ticket**: CASSANDRA-3500

## Detection Rule

When a SQL-like tool supports `schema.table` qualified names, if the keyspace qualifier from the parsed statement is not extracted and passed to schema lookup, the lookup will always use the session's current keyspace, returning wrong results for cross-keyspace references.

## Code Smell

`self.determine_decoder_for(cfname)` called without passing the parsed keyspace qualifier from the SELECT statement, so `determine_decoder_for` always falls back to `self.current_keyspace`.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: missing-qualified-name-propagation
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause

In cqlsh's `do_select()`, the parsed `selectks` binding (keyspace prefix from `keyspace.cfname` syntax) was not extracted or forwarded to `determine_decoder_for()`. That method hardcoded `self.current_keyspace` for schema override lookups, so `ASSUME` type hints applied to a qualified table name would never match. The fix extracts `ksname` from the binding and passes it through.

## Example

```python
# BEFORE
cfname = cql_dequote(parsed.get_binding('selectsource'))
decoder = self.determine_decoder_for(cfname)  # always uses current_keyspace

def determine_decoder_for(self, cfname):
    schema = self.schema_overrides.get((self.current_keyspace, cfname), None)
```

```python
# AFTER
ksname = parsed.get_binding('selectks')
if ksname is not None:
    ksname = cql_dequote(ksname)
cfname = cql_dequote(parsed.get_binding('selectsource'))
decoder = self.determine_decoder_for(cfname, ksname=ksname)

def determine_decoder_for(self, cfname, ksname=None):
    if ksname is None:
        ksname = self.current_keyspace
    schema = self.schema_overrides.get((ksname, cfname), None)
```
