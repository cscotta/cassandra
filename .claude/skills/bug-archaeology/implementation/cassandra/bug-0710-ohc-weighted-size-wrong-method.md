# Bug 0710: weightedSize() Returns Entry Count Instead of Memory Usage for Row Cache

**Commit**: 470f15be65 · **Ticket**: CASSANDRA-13393
**Subsystem**: Cache / Metrics (JMX / nodetool)
**Category**: Wrong constant / default value

---

## Root Cause
`OHCProvider.OHCacheAdapter.weightedSize()` called `ohCache.size()`, which returns the number of entries in the off-heap cache, rather than `ohCache.memUsed()`, which returns the actual number of bytes consumed. As a result, JMX metrics and `nodetool info` reported the row-cache "capacity used" figure in units of entry count rather than bytes, making the metric meaningless and orders of magnitude too small.

### Before / After
```java
// BEFORE
public long weightedSize()
{
    return ohCache.size();     // returns entry count (e.g., 1000)
}

// AFTER
public long weightedSize()
{
    return ohCache.memUsed();  // returns bytes (e.g., 4194304)
}
```

---

## How to Spot
When implementing a `weightedSize()` method for a cache backed by an off-heap or custom allocator, verify that the returned value is in the same unit (bytes) as the capacity it is compared against. Method names like `size()` commonly return element counts, not memory consumption — confirm the unit by checking the underlying library's API documentation or source.

---

## Test
No test added.
