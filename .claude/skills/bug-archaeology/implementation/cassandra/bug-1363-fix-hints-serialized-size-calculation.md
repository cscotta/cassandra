# Bug 1363: Fix hints serialized size calculation

**Commit**: e4fc395242 · **Ticket**: CASSANDRA-8587
**Subsystem**: src/java/org/apache/cassandra/net/MessageOut.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`MessageOut.serializedSize()` computed the byte size of the message's parameter map by iterating over each entry and calling `TypeSizes.NATIVE.sizeof(key)` and `TypeSizes.NATIVE.sizeof(value.length)` — but never accumulated the return values into `size`. The two calls were pure expressions whose results were discarded, so the map header (number of entries) was counted but each entry's key and value-length prefix were not. The resulting under-count caused hints to be allocated or framed with too-small buffers, leading to serialization errors or data corruption on hinted handoff writes.

### Before / After

```java
// BEFORE — return values of sizeof() silently discarded
TypeSizes.NATIVE.sizeof(entry.getKey());
TypeSizes.NATIVE.sizeof(entry.getValue().length);
size += entry.getValue().length;
```

```java
// AFTER — size properly accumulated
size += TypeSizes.NATIVE.sizeof(entry.getKey());
size += TypeSizes.NATIVE.sizeof(entry.getValue().length);
size += entry.getValue().length;
```

---

## How to Spot

Any call to a side-effect-free `sizeof`/`sizeOf`/`measure` method whose return value is not used is a dead computation and almost certainly a bug. Static analysis ("result of method call is ignored") or a simple grep for `TypeSizes.*sizeof` lines that are not assignments will surface this pattern immediately. Code review should flag any sizing loop where individual field contributions are not visibly added to the accumulator.

---

## Test

No dedicated test added; the fix was validated against hinted handoff replay.
