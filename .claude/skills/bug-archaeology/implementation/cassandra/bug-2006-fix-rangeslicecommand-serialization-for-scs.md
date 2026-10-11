# Bug 2006: RangeSliceCommand Serializer Used Wrong Filter Variable for SuperColumn Nodes

**Commit**: fa7d87de8d · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/RangeSliceCommand.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

In `RangeSliceCommandSerializer.serialize()`, when serializing a `RangeSliceCommand` for a 1.2 node that uses SuperColumns, the code correctly built a `filter` local variable to hold the adapted predicate, but then accidentally passed `sliceCommand.predicate` (the original, unadapted predicate) to `IDiskAtomFilter.Serializer.instance.serialize()`. This meant the wrong (possibly incompatible) filter was sent over the wire to 1.2 nodes during mixed-version rolling upgrades.

### Before / After

```java
// BEFORE
IDiskAtomFilter.Serializer.instance.serialize(sliceCommand.predicate, out, version);
```

```java
// AFTER
IDiskAtomFilter.Serializer.instance.serialize(filter, out, version);
```

---

## How to Spot

When a local variable is constructed specifically to hold an adapted or transformed version of a field, double-check that the local variable — not the original field — is used in all subsequent operations. A variable that is computed but never used after construction is a strong indicator of this bug.

---

## Test

No test added.
