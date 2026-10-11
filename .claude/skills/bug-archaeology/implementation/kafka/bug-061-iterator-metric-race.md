**Commit**: 8015c87390 · **Ticket**: KAFKA-19679
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/internals/metrics/OpenIterators.java
**Category**: Race condition / missing synchronization

**Root cause**
The `oldest-open-iterator` gauge directly called `openIterators.first()` at query time. Between the `isEmpty()` check inside the gauge lambda and the `first()` call, another thread could call `remove()` on the last iterator, making the set empty and causing `NoSuchElementException`. The fix decouples the metric value from direct set access by caching the oldest start timestamp in an `AtomicLong` that is updated whenever the set changes, making the gauge read a single atomic value instead of traversing the live concurrent set.

**Before/After**
```java
// Before: gauge reads directly from the live set — TOCTOU race
metricName = StateStoreMetrics.addOldestOpenIteratorGauge(...,
    (config, now) -> openIterators.first().startTimestamp()  // race: set may be empty
);

// After: gauge reads a cached AtomicLong updated on every add/remove
private final AtomicLong oldestStartTimestamp = new AtomicLong();

// In add() and remove():
private void updateOldestStartTimestamp() {
    Iterator<MeteredIterator> it = openIterators.iterator();
    if (it.hasNext()) oldestStartTimestamp.set(it.next().startTimestamp());
}
// gauge reads: (config, now) -> oldestStartTimestamp.get()
```

**How to spot**
Any metric gauge that calls a method like `first()`, `min()`, or `iterator().next()` on a live concurrent collection is vulnerable to TOCTOU races. Prefer caching the computed metric value in an atomic variable and updating it eagerly on mutations. Look for lambda captures of concurrent collections in `Gauge` or `Measurable` implementations.

**Test**
`OpenIteratorsTest.shouldCalculateOldestStartTimestampCorrectly` adds three iterators with timestamps 5, 2, and 6, removes them one by one, and asserts the gauge returns the correct minimum at each step without throwing.
