# Bug 0940: BTreeRow.hashCode includes unstable columns() set

**Commit**: f6af142f1e · **Ticket**: CASSANDRA-11316
**Subsystem**: Storage / Row Model
**Category**: Logic error in condition

---

## Root Cause
`AbstractRow.hashCode()` fed `columns()` — the live set of columns present in the row — into `Objects.hash()` alongside `clustering()`, `primaryKeyLivenessInfo()`, and `deletion()`. Because `columns()` is derived from the row's actual cell contents, two logically equal rows built from different construction paths (e.g., one from an SSTable read and one from a `BTreeRow.Builder`) could produce different `columns()` objects even though they held identical cells. The column-level contribution was already captured by iterating over `ColumnData` items at the end of the method; including `columns()` in the initial hash was redundant and made the result depend on the internal `ColumnData` collection type rather than content, breaking contract.

### Before / After
```java
// BEFORE — columns() included in seed hash, making it unstable
int hash = Objects.hash(clustering(), columns(), primaryKeyLivenessInfo(), deletion());
for (ColumnData cd : this)
    hash += 31 * cd.hashCode();
return hash;

// AFTER — columns() removed; column content already covered by ColumnData loop
int hash = Objects.hash(clustering(), primaryKeyLivenessInfo(), deletion());
for (ColumnData cd : this)
    hash += 31 * cd.hashCode();
return hash;
```

---

## How to Spot
When a `hashCode()` iterates a collection of items that each contribute their own hash, seeding the hash with a derived summary of those same items (like `columns()`) double-counts the content in a type-dependent way and can violate the `equals`/`hashCode` contract if the summary uses identity rather than content equality. Always verify that every field contributing to `hashCode` uses value equality and that no field is counted twice.

---

## Test
`RowTest` was extended with a test that constructs two logically identical rows via different paths and asserts their hash codes are equal.
