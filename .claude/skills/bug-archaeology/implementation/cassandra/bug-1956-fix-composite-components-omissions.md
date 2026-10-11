# Bug 1956: Secondary index support on composite clustering columns was incomplete

**Commit**: 703bd684ca · **Ticket**: CASSANDRA-5851
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java, src/java/org/apache/cassandra/cql3/statements/CreateIndexStatement.java, src/java/org/apache/cassandra/db/index/composites/CompositesIndexOnClusteringKey.java, pylib/cqlshlib/cql3handling.py, bin/cqlsh
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Secondary index handling in CQL3 for composite (clustering-key) columns had multiple omissions: the Java `SelectStatement` and `CreateIndexStatement` used stale or incorrect column metadata lookups for composite components, `CompositesIndexOnClusteringKey` had a logic gap preventing correct index entry construction, and the Python cqlsh library referenced deprecated `column_aliases`/`partition_key_components` attributes instead of the updated `clustering_key_columns`/`partition_key_columns` API. Together these caused 2i queries on clustering-key columns to produce wrong or missing results and cqlsh output to be malformed.

### Before / After

```python
# BEFORE (cqlshlib)
order_by_candidates = layout.column_aliases[:]
filterable = set((layout.partition_key_components[0], layout.column_aliases[0]))
```

```python
# AFTER (cqlshlib)
order_by_candidates = layout.clustering_key_columns[:]
filterable = set((layout.partition_key_columns[0], layout.clustering_key_columns[0]))
```

---

## How to Spot

When a data-model abstraction is renamed or refactored (e.g. `column_aliases` → `clustering_key_columns`), search for every call site across all language layers (Java, Python, etc.) before merging; a missed rename leaves the old code silently querying the wrong field.

---

## Test

`pylib/cqlshlib/test/test_cqlsh_output.py` updated with new expected output for affected scenarios.
