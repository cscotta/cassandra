# Bug 1762: isDeterministic Always Returns false; CqlReader Result Row Includes Key Column

**Commit**: c869087267 · **Ticket**: n/a
**Subsystem**: tools/stress (RowGenDistributedSize, CqlOperation)
**Category**: Logic error in condition

---

## Root Cause

Two separate bugs were fixed together in this merge commit. First, `RowGenDistributedSize.isDeterministic()` unconditionally returned `false`, preventing stress from recognizing when a row generator is actually deterministic (i.e., when column count and column size are fixed and the underlying data generator is also deterministic). This caused unnecessary re-generation of data that could have been reused.

Second, `CqlOperation`'s result validation logic compared the full result row (including the partition key column at index 0) against the expected value list, which did not include the key. The fix strips the first element (the key column) from the result row before comparison, and also skips the equality check when the expected row is `null`.

### Before / After

```java
// BEFORE — RowGenDistributedSize
@Override
public boolean isDeterministic()
{
    return false;
}
```

```java
// AFTER — RowGenDistributedSize
final boolean isDeterministic;
// (computed in constructor)
this.isDeterministic = dataGen.isDeterministic()
    && countDistribution.maxValue() == countDistribution.minValue()
    && sizeDistribution.minValue() == sizeDistribution.maxValue();

@Override
public boolean isDeterministic()
{
    return isDeterministic;
}
```

```java
// BEFORE — CqlOperation result check
if (!expect.get(i).equals(Arrays.asList(result[i])))
    return false;
```

```java
// AFTER — CqlOperation result check
List<ByteBuffer> resultRow = Arrays.asList(result[i]);
resultRow = resultRow.subList(1, resultRow.size()); // drop key column
if (expect.get(i) != null && !expect.get(i).equals(resultRow))
    return false;
```

---

## How to Spot

When `isDeterministic()` or any similar predicate method always returns a hard-coded constant, verify that the constant is correct for all possible configurations — it likely should be computed from the object's fields. For result comparisons against a known schema, confirm that result columns and expected columns have the same cardinality before comparing.

---

## Test

No test added.
