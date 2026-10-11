# Bug 1890: Fix trace activity column in TraceState

**Commit**: 5d5925aaf4 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/tracing
**Category**: Logic error in condition

---

## Root Cause

Inside `TraceState`'s async trace event writer, the `thread` column was written twice—once correctly, and then again in a copy-paste duplicate that overwrote what should have been the `activity` column. The actual trace message (the human-readable description of what happened) was therefore never stored, making trace records useless for diagnosing slow queries.

### Before / After

```java
// BEFORE
Tracing.addColumn(cf, Tracing.buildName(cfMeta, eventId, ByteBufferUtil.bytes("thread")), threadName);
if (elapsed >= 0)
    Tracing.addColumn(cf, Tracing.buildName(cfMeta, eventId, ByteBufferUtil.bytes("source_elapsed")), elapsed);
Tracing.addColumn(cf, Tracing.buildName(cfMeta, eventId, ByteBufferUtil.bytes("thread")), threadName); // duplicate
```

```java
// AFTER
Tracing.addColumn(cf, Tracing.buildName(cfMeta, eventId, ByteBufferUtil.bytes("thread")), threadName);
if (elapsed >= 0)
    Tracing.addColumn(cf, Tracing.buildName(cfMeta, eventId, ByteBufferUtil.bytes("source_elapsed")), elapsed);
Tracing.addColumn(cf, Tracing.buildName(cfMeta, eventId, ByteBufferUtil.bytes("activity")), message);
```

---

## How to Spot

When multiple `addColumn` calls share the same column-name literal, that is a strong signal of copy-paste error. During review, verify that every logical column in the trace schema is written exactly once with a distinct name constant.

---

## Test

No test added.
