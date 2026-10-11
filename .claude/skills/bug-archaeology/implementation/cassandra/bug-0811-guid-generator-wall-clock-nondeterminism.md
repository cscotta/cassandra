# Bug 0811: GuidGenerator uses wall-clock time, making token allocation non-deterministic in tests

**Commit**: c6ec31bada · **Ticket**: CASSANDRA-12784
**Subsystem**: Token Allocation / Partitioner
**Category**: Wrong constant / default value

---

## Root Cause
`GuidGenerator.guidAsBytes(Random)` internally called `System.currentTimeMillis()` to obtain a time component that was mixed into the GUID bytes fed to the MD5 hash. When the token allocator test seeded its `Random` instance to obtain deterministic output, the wall-clock `currentTimeMillis()` call still injected real-world time, making consecutive test runs produce different token sequences and causing test timeouts. The fix separates the time source from the random source: the caller now supplies the `long time` argument, and the no-argument `guidAsBytes()` passes `System.currentTimeMillis()` from the call site, while `RandomPartitioner.getRandomToken(Random)` passes `0` to produce fully reproducible tokens during tests.

### Before / After
```java
// BEFORE — time baked in, invisible to callers
public static ByteBuffer guidAsBytes(Random random) {
    long time = System.currentTimeMillis();
    ...
}

// AFTER — caller controls the time
public static ByteBuffer guidAsBytes(Random random, long time) { ... }

public static ByteBuffer guidAsBytes() {
    return guidAsBytes(myRand, System.currentTimeMillis());
}

// In RandomPartitioner — pass 0 for deterministic hashing
BigInteger token = FBUtilities.hashToBigInteger(GuidGenerator.guidAsBytes(random, 0));
```

---

## How to Spot
Utility methods that silently read wall-clock time are a hidden source of non-determinism. When a method accepts a `Random` for reproducibility but also calls `System.currentTimeMillis()` or `System.nanoTime()` internally, callers cannot produce deterministic output even with a fixed seed. Flag any helper that mixes a seeded `Random` with a live time source as a candidate for parameter extraction.

---

## Test
`ReplicationAwareTokenAllocatorTest` (renamed from a single-class file) was split into `Murmur3ReplicationAwareTokenAllocatorTest` and `RandomReplicationAwareTokenAllocatorTest`, and the base class was refactored to run correctly within CI time bounds with the now-deterministic token generation.
