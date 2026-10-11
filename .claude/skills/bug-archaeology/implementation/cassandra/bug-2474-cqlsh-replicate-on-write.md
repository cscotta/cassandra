# Bug 2474: Wrong option key name for replicate_on_write in cqlsh

**Commit**: 24f8ddeea1 · **Ticket**: CASSANDRA-3596

## Detection Rule

When a CLI/tool maps an internal option name to a user-facing name using a tuple `(internal, alias)`, swapping the two or using the wrong internal name silently ignores the option.

## Code Smell

A tuple `('replication_on_write', 'replicate_on_write')` where the first element (the actual option name) is misspelled; the option will never match and will be silently ignored.

## Tags

- scope: cassandra
- category: wrong-constant
- code-pattern: misspelled-option-key
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause

In cqlsh's `columnfamily_options` list, the entry `('replication_on_write', 'replicate_on_write')` had the option key spelled as `replication_on_write` (wrong) instead of `replicate_on_write` (correct). This prevented the option from being recognized during `CREATE`/`ALTER` completion and parsing.

## Example

```python
# BEFORE
('replication_on_write', 'replicate_on_write')
```

```python
# AFTER
('replicate_on_write', None)
```
