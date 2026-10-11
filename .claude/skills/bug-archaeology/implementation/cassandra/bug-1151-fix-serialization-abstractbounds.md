# Bug 1151: AbstractBounds Serialization Loses Inclusivity Information for IncludingExcludingBounds and ExcludingBounds

**Commit**: bd7d1198ac · **Ticket**: CASSANDRA-9775
**Subsystem**: src/java/org/apache/cassandra/dht/AbstractBounds.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

The pre-3.0 `AbstractBounds` wire format encoded bounds type as a single signed integer that could only distinguish `Range` (exclusive start, inclusive end) from `Bounds` (inclusive start, inclusive end). When 3.0 introduced `IncludingExcludingBounds` (inclusive start, exclusive end) and `ExcludingBounds` (exclusive start, exclusive end), the serializer was never updated: both new types were silently round-tripped as `Bounds`, corrupting their end-inclusivity semantics. The fix introduces a 3.0+ wire format that uses a single flag byte encoding three independent bits—IS_TOKEN, START_INCLUSIVE, END_INCLUSIVE—allowing all four concrete bound types to survive serialization faithfully. The old integer format is preserved for peers running older protocol versions.

### Before / After

```java
// BEFORE – kind integer can only encode Range vs. Bounds; new types silently become Bounds
out.writeInt(kindInt(range));
// ...
if (kind == Type.RANGE.ordinal())
    return new Range<T>(left, right);
return new Bounds<T>(left, right);
```

```java
// AFTER – 3.0+ uses a flag byte with independent start/end inclusive bits
if (version < MessagingService.VERSION_30)
    out.writeInt(kindInt(range));
else
    out.writeByte(kindFlags(range));
// ...
if (startInclusive)
    return endInclusive ? new Bounds<T>(left, right) : new IncludingExcludingBounds<T>(left, right);
else
    return endInclusive ? new Range<T>(left, right) : new ExcludingBounds<T>(left, right);
```

---

## How to Spot

Whenever a type hierarchy adds a new concrete subclass, every serializer that uses a type-ordinal or enum-to-class mapping must be audited to cover the new case—an exhaustive switch or explicit enum mapping makes missing cases a compile error. Reviewers should verify that the deserialize side constructs the exact same runtime type that the serialize side wrote, not a related but distinct type.

---

## Test

No test added.
