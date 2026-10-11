# Bug 1213: Mixing min, max, and count Aggregates Fails for Blob Type

**Commit**: 716b253a77 · **Ticket**: CASSANDRA-9622
**Subsystem**: src/java/org/apache/cassandra/cql3/functions/Functions.java
**Category**: Logic error in condition

---

## Root Cause

In `Functions.addFunctionsMaybeUpdateExisting()`, a single loop over `CQL3Type.Native.values()` registered both the blob-conversion functions and the aggregate functions (`count`, `min`, `max`). The loop body skipped `VARCHAR` and `BLOB` for blob conversions with `continue`, which also skipped registration of the aggregate functions for those types. As a result, `min`, `max`, and `count` were never declared for `BLOB`, making queries that mix those aggregates with blob columns fail. The fix separates blob-conversion registration from aggregate registration into two independent loops, giving each its own skip condition.

### Before / After

```java
// BEFORE
for (CQL3Type type : CQL3Type.Native.values())
{
    if (type == CQL3Type.Native.VARCHAR || type == CQL3Type.Native.BLOB)
        continue;   // <-- also skips aggregate registration for BLOB

    declare(BytesConversionFcts.makeToBlobFunction(type.getType()));
    declare(BytesConversionFcts.makeFromBlobFunction(type.getType()));

    declare(AggregateFcts.makeCountFunction(type.getType()));
    declare(AggregateFcts.makeMaxFunction(type.getType()));
    declare(AggregateFcts.makeMinFunction(type.getType()));
}
```

```java
// AFTER
for (CQL3Type type : CQL3Type.Native.values())
{
    if (type != CQL3Type.Native.VARCHAR && type != CQL3Type.Native.BLOB)
    {
        declare(BytesConversionFcts.makeToBlobFunction(type.getType()));
        declare(BytesConversionFcts.makeFromBlobFunction(type.getType()));
    }
}

for (CQL3Type type : CQL3Type.Native.values())
{
    if (type != CQL3Type.Native.VARCHAR)  // skip only to avoid VARCHAR/UTF8 duplicate
    {
        declare(AggregateFcts.makeCountFunction(type.getType()));
        declare(AggregateFcts.makeMaxFunction(type.getType()));
        declare(AggregateFcts.makeMinFunction(type.getType()));
    }
}
```

---

## How to Spot

When a single loop body handles multiple independent concerns, a `continue` or early-exit for one concern silently suppresses all subsequent work in the same iteration. Reviewers should confirm that each guard/skip is scoped only to the code it logically protects, and that unrelated registrations or side-effects in the same loop body are not inadvertently gated by the same condition.

---

## Test

No dedicated test was added in this commit.
