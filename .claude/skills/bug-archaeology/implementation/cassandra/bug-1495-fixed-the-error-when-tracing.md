# Bug 1495: Fixed the Error When Tracing Query with cqlsh

**Commit**: 91e63d0546 · **Ticket**: CASSANDRA-7613
**Subsystem**: bin/cqlsh
**Category**: Missing null / bounds check

---

## Root Cause

`Shell.myformat_colname()` always attempted to look up the column name in `cfMetaData.partition_key` and `cfMetaData.clustering_key`, but the method was called without a `cfMetaData` argument when formatting tracing output (where no column family context is available). Because Python's default argument was absent, the parameter was required, but the tracing code path passed `None` (or nothing), causing an `AttributeError` when the code tried to iterate `cfMetaData.partition_key`. The fix makes `cfMetaData` optional (defaulting to `None`) and guards the key-coloring logic behind an `if cfMetaData:` check.

### Before / After

```python
# BEFORE
def myformat_colname(self, name, cfMetaData):
    column_colors = COLUMN_NAME_COLORS.copy()
    if name in [col.name for col in cfMetaData.partition_key]:   # AttributeError if None
        column_colors.default_factory = lambda : RED
    elif name in [col.name for col in cfMetaData.clustering_key]:
        column_colors.default_factory = lambda : CYAN
    return self.myformat_value(name, colormap=column_colors)
```

```python
# AFTER
def myformat_colname(self, name, cfMetaData = None):
    column_colors = COLUMN_NAME_COLORS.copy()
    if cfMetaData:
        if name in [col.name for col in cfMetaData.partition_key]:
            column_colors.default_factory = lambda : RED
        elif name in [col.name for col in cfMetaData.clustering_key]:
            column_colors.default_factory = lambda : CYAN
    return self.myformat_value(name, colormap=column_colors)
```

---

## How to Spot

When a helper method is called from multiple code paths, some of which lack a particular piece of context, that context argument should be optional and the method body should guard all uses of it. Required parameters that are only meaningful in some call sites are a common source of `None`/null dereference bugs.

---

## Test

No test added.
