# Bug 1234: Fix Potential StackOverflow When Setting CrcCheckChance over JMX

**Commit**: 04eba90949 · **Ticket**: CASSANDRA-9488
**Subsystem**: src/java/org/apache/cassandra/io/compress/CompressionParameters.java
**Category**: Logic error in condition

---

## Root Cause

`CompressionParameters.setCrcCheckChance` propagates the new value to a `liveMetadata` companion object so that live SSTables stay in sync with configuration changes made via JMX. However, when `this` and `liveMetadata.compressionParameters` are the same object (a valid state that occurs under certain metadata configurations), calling `liveMetadata.compressionParameters.setCrcCheckChance(...)` re-enters the same method on the same instance, producing infinite recursion and a `StackOverflowError`. The fix adds an identity check before the recursive call.

### Before / After

```java
// BEFORE
if (liveMetadata != null)
    liveMetadata.compressionParameters.setCrcCheckChance(crcCheckChance);
```

```java
// AFTER
if (liveMetadata != null && this != liveMetadata.compressionParameters)
    liveMetadata.compressionParameters.setCrcCheckChance(crcCheckChance);
```

---

## How to Spot

Any setter that delegates to another object's same setter must guard against the case where the two objects are identical. When reviewing "fan-out" or "propagate-to-sibling" patterns, check whether the target can ever be `this`. A missing identity check (`this != target`) in a mutually-referencing graph turns a propagation into unbounded recursion.

---

## Test

No test added.
