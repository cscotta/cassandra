# Bug 0899: DatabaseDescriptor Static Reference in AbstractType Breaks Client-Mode Initialization

**Commit**: 57ead56f3b · **Ticket**: CASSANDRA-11912
**Subsystem**: Type System / Serialization
**Category**: State not cleaned up

---

## Root Cause
`AbstractType` stored a `maxValueSize` field that was populated in the base constructor by calling `DatabaseDescriptor.getMaxValueSize()` at type-singleton construction time. Because `AbstractType` instances are global singletons, this call happened during class loading — before `DatabaseDescriptor` was necessarily initialized in non-server contexts (e.g. sstable tools running in client mode). It also meant the per-type limit was frozen at construction time and could diverge from a later `DatabaseDescriptor` configuration change. The fix removes the cached field and instead passes the limit explicitly at every `readValue` call site via a new `readValue(in, maxValueSize)` overload, with `readValue(in)` delegating to `Integer.MAX_VALUE` (no effective limit) as a safe default.

### Before / After
```java
// BEFORE — AbstractType constructor caches DatabaseDescriptor value at class-load time
protected AbstractType(ComparisonType comparisonType)
{
    this(comparisonType, DatabaseDescriptor.getMaxValueSize());
}
protected AbstractType(ComparisonType comparisonType, int maxValueSize)
{
    ...
    this.maxValueSize = maxValueSize;
}

// AFTER — no cached field; limit passed at call time
protected AbstractType(ComparisonType comparisonType)
{
    this.comparisonType = comparisonType;
    ...
}
public ByteBuffer readValue(DataInputPlus in, int maxValueSize) throws IOException { ... }
public ByteBuffer readValue(DataInputPlus in) throws IOException
{
    return readValue(in, Integer.MAX_VALUE);
}
```

---

## How to Spot
Any base class or singleton that reads from `DatabaseDescriptor` (or any other global mutable config object) in a constructor is a latent initialization-order hazard. Review usages where `AbstractType` subclass singletons are referenced in static initializers or tool-mode code paths. The remediation pattern is always the same: remove cached config fields from long-lived singletons and pass configuration as explicit parameters to the methods that actually need it.

---

## Test
Existing tests updated to call `DatabaseDescriptor.setMaxValueSize(...)` globally instead of mutating per-column-type instances.
