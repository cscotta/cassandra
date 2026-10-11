# Bug 1018: Fix completion problems in cqlsh

**Commit**: 1b81ad19d3 · **Ticket**: CASSANDRA-10753
**Subsystem**: CQL Shell (cqlsh)
**Category**: Wrong constant / default value

---

## Root Cause
The cqlsh completion logic was written against the old cassandra-driver API where column metadata exposed `.data_type.typename` (a string name) and `.index` (an attribute directly on the column). The driver was updated so that column types are accessed via `.cql_type` directly, and index information moved to `table_meta.indexes` (a separate dict). All completion completers that accessed `.data_type.typename` or `.data_type.cql_parameterized_type()` were broken, producing `AttributeError` at runtime. Additionally, the partition-key and clustering-key completer started its range at index 1, so the first key component was never added to the `filterable` set.

### Before / After
```python
# BEFORE — wrong attribute access, off-by-one in key range, stale index API
filterable = set((layout.partition_key[0].name, layout.clustering_key[0].name))
for num in range(1, len(layout.partition_key)):          # misses index 0
    if layout.partition_key[num - 1].name in already_filtered_on:
        filterable.add(layout.partition_key[num].name)
for cd in layout.columns.values():
    if cd.index:                                          # old API
        filterable.add(cd.name)
coltype = layout.columns[curcol].data_type.typename       # old API

# AFTER — correct attribute, full range from 0, new index API
filterable = set()
for num in range(0, len(layout.partition_key)):
    if num == 0 or layout.partition_key[num - 1].name in already_filtered_on:
        filterable.add(layout.partition_key[num].name)
for idx in layout.indexes.itervalues():
    filterable.add(idx.index_options["target"])
coltype = layout.columns[curcol].cql_type                 # new API
```

---

## How to Spot
Driver API migrations frequently leave behind calls to renamed or removed attributes. In code review, check that every attribute access on driver metadata objects (`ColumnMetadata`, `TableMetadata`, `IndexMetadata`) matches the driver version bundled with the release. The off-by-one was masked by pre-seeding the filterable set with the first key column names, hiding the fact that the loop-based logic was logically incorrect for all subsequent columns too.

---

## Test
`test_cqlsh_completion.py` was updated to cover the corrected completion scenarios.
