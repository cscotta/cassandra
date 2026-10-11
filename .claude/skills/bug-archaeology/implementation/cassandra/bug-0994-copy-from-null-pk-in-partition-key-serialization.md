# Bug 0994: COPY FROM Misses Null Primary Key in Partition Key Serialization

**Commit**: 3ccffc94b8 · **Ticket**: CASSANDRA-10854
**Subsystem**: CQL shell / COPY FROM
**Category**: Missing null / bounds check

---

## Root Cause
`ImportConversion.get_row_partition_key_values` serializes each column in the partition key to build the binary token for row routing. It called the per-column converter and `cqltypes.serialize` directly without first checking whether the value was the configured null marker. When a CSV row contained consecutive delimiters (e.g. `,,`) one of the delimiters produced an empty-string value that resolved to `nullval`. The null check for primary key columns existed only in `get_row_data`, the method that converts all columns for the actual INSERT. Because `get_row_partition_key_values` is called separately—before the full row is even converted—the null value was silently serialized as if it were a real value, or caused an obscure serialization error rather than the intended "Cannot insert null value for primary key column" message.

### Before / After
```python
# BEFORE — no null check before serialization
def serialize(n):
    c, v = self.columns[n], row[n]
    return self.cqltypes[c].serialize(self.converters[c](v), self.proto_version)

# AFTER — null guard added before serialization
def serialize(n):
    c, v = self.columns[n], row[n]
    if v == self.nullval:
        raise ValueError(self.get_null_primary_key_message(n))
    return self.cqltypes[c].serialize(self.converters[c](v), self.proto_version)
```

---

## How to Spot
Whenever validation logic and processing logic operate on the same input through separate code paths, check that every path performs the full set of invariant checks. Here the null-value guard was duplicated in `get_row_data` but absent in `get_row_partition_key_values`. Review any place where partition key values are accessed without going through the central conversion method, since those bypasses tend to skip input validation.

---

## Test
No test added.
