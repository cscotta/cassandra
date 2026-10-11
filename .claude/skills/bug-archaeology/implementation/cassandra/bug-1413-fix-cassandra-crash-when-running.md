# Bug 1413: Fix Cassandra Crash When Running on JDK8 Update 40

**Commit**: 4f863be3f7 · **Ticket**: CASSANDRA-8209
**Subsystem**: src/java/org/apache/cassandra/utils/ObjectSizes.java, src/java/org/apache/cassandra/cql3/QueryProcessor.java
**Category**: Wrong constant / default value

---

## Root Cause

JDK 8 update 40 changed the internal representation of certain singleton objects (e.g., `Boolean.TRUE`, `Boolean.FALSE`, JVM-internal classes), causing `jamm`'s `MemoryMeter` to follow those references and attempt to measure shared JVM singletons. The old version of jamm (0.2.6) did not support `ignoreKnownSingletons()`, and the upgrade to jamm 0.2.8 introduced that capability. Without ignoring known singletons, `MemoryMeter` would traverse into restricted JVM internals and crash with an exception during object size measurement in both `ObjectSizes` and the CQL prepared statement cache weigher.

### Before / After

```java
// BEFORE (ObjectSizes.java)
private static final MemoryMeter meter = new MemoryMeter()
    .omitSharedBufferOverhead()
    .withGuessing(MemoryMeter.Guess.FALLBACK_UNSAFE);

// BEFORE (QueryProcessor.java)
private static final MemoryMeter meter =
    new MemoryMeter().withGuessing(MemoryMeter.Guess.FALLBACK_BEST);
```

```java
// AFTER (ObjectSizes.java)
private static final MemoryMeter meter = new MemoryMeter()
    .omitSharedBufferOverhead()
    .withGuessing(MemoryMeter.Guess.FALLBACK_UNSAFE)
    .ignoreKnownSingletons();

// AFTER (QueryProcessor.java)
private static final MemoryMeter meter =
    new MemoryMeter().withGuessing(MemoryMeter.Guess.FALLBACK_BEST)
                     .ignoreKnownSingletons();
```

---

## How to Spot

When a JVM or library upgrade changes how shared/singleton objects are traversed during deep object graph walks, any `MemoryMeter` (or similar reflective size estimator) without singleton guards becomes a crash risk. Review all `MemoryMeter` construction sites to ensure `ignoreKnownSingletons()` is called, particularly after upgrading jamm or moving to a new JDK minor version.

---

## Test

No test added.
