# Bug 0024: Lambda Capture Allocation in UnfilteredSerializer.deserializeRowBody

**Commit**: b09bc8b8b0 · **Ticket**: CASSANDRA-21289
**Subsystem**: db/rows/UnfilteredSerializer
**Category**: Resource leak

---

## Root Cause

`UnfilteredSerializer.deserializeRowBody` iterated over columns by passing a lambda closure to `Columns.apply`. Because the lambda captured several local variables (`in`, `header`, `builder`, `livenessInfo`), the JVM was forced to allocate a new lambda instance on every call to `deserializeRowBody`. This is the read hot-path executed for every row deserialized from disk, so the allocation pressure was significant. The companion serialization path `serializeRowBody` had already been fixed by the same technique (state-passing via an existing helper object), but the fix was not applied to the deserialization path.

### Before / After

```java
// BEFORE
DataInputPlus finalIn = in;
columns.apply(column -> {
    if (column.isSimple())
        readSimpleColumn(column, finalIn, header, helper, builder, livenessInfo);
    else
        readComplexColumn(column, finalIn, header, helper, hasComplexDeletion, builder, livenessInfo);
});
```

```java
// AFTER
helper.in = in;
helper.header = header;
helper.builder = builder;
helper.livenessInfo = livenessInfo;
helper.hasComplexDeletion = hasComplexDeletion;
columns.apply(UnfilteredSerializer::readColumn, helper);
```

---

## How to Spot

When a lambda captures local variables, a new object is allocated per invocation instead of using a cached singleton. In a hot deserialization loop this silently inflates GC pressure without any functional breakage. Review any `Collection.apply` / `forEach` call in a tight loop that captures more than one variable — if an existing context object is available as a parameter, threading state through it avoids the allocation entirely.

---

## Test

No dedicated test added; the fix is a pure allocation-avoidance refactor validated by the pre-existing serialization round-trip tests.
