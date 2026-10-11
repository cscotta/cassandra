# Bug 0580: Chunk Cache Metrics Double-Count Requests and Miss Latency Not Recorded

**Commit**: e6b8e7a72f · **Ticket**: CASSANDRA-14628
**Subsystem**: src/java/org/apache/cassandra/cache/ChunkCache.java, src/java/org/apache/cassandra/metrics/
**Category**: Wrong constant / default value

---

## Root Cause

The old `ChunkCache` implementation manually called `metrics.requests.mark()` inside the read path and `metrics.misses.mark()` plus a `missLatency` timer in `load()`. This duplicated the accounting that Caffeine's own stats infrastructure could provide via a `StatsCounter` callback, and it computed hit rate as `(requests - misses) / requests` rather than tracking hits directly. The `InstrumentingCache` (for row/key caches) had the inverse problem: it marked requests but not misses, so misses were unreported. The fix wires `ChunkCacheMetrics` as a Caffeine `StatsCounter`, tracks `hits` and `misses` as separate meters whose sum becomes `requests`, and removes all manual marking.

### Before / After

```java
// BEFORE (load): manual miss tracking inside load()
metrics.misses.mark();
try (Timer.Context ctx = metrics.missLatency.time()) {
    rebufferer.readChunk(key.position, buffer);
}
// BEFORE (get): manual request tracking
metrics.requests.mark();
```

```java
// AFTER: Caffeine callbacks drive all metrics
cache = Caffeine.newBuilder()
    ...
    .recordStats(() -> metrics)   // StatsCounter wired here
    .build(this);
// load() has no metrics calls; recordMisses/recordLoadSuccess called by Caffeine
```

---

## How to Spot

When integrating a caching library that has its own statistics collection API (Caffeine's `StatsCounter`, Guava's `CacheStats`), avoid mixing manual `mark()` calls with the library's built-in hooks — the two will double-count or produce inconsistent hit/miss rates. Always delegate statistics to one authoritative source.

---

## Test

No test added.
