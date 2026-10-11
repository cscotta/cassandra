# Bug 1397: Prepared Statement Size Calculations Wildly Inflated Due to Old jamm Version

**Commit**: 1bb2dd906c · **Ticket**: CASSANDRA-8231
**Subsystem**: src/java/org/apache/cassandra/cql3/QueryProcessor.java
**Category**: Wrong constant / default value

---

## Root Cause

The prepared statement cache used `jamm` 0.2.8 to measure the deep memory size of cached statements. That version of jamm incorrectly traversed and counted shared objects that should have been excluded — in particular `CFMetaData`, `AbstractType` instances, and `Function` objects which are effectively global singletons. The result was that every cached statement appeared many times larger than it actually was, causing the LRU cache to evict statements far too aggressively. The fix upgrades to jamm 0.3.0, which supports the `@Unmetered` annotation, and annotates the shared singletons so they are excluded from deep measurement. The per-class `MeasurableForPreparedCache` interface with hand-rolled measurement methods was also removed entirely in favor of simply calling `meter.measureDeep()` on the whole object graph now that shared types are properly excluded.

### Before / After

```java
// BEFORE: hand-rolled measurement, but no exclusion of shared singletons
private static long measure(Object key)
{
    return key instanceof MeasurableForPreparedCache
         ? ((MeasurableForPreparedCache)key).measureForPreparedCache(meter)
         : meter.measureDeep(key);
}
```

```java
// AFTER: shared types annotated @Unmetered; just measure the whole graph
private static long measure(Object key)
{
    return meter.measureDeep(key);
}
```

---

## How to Spot

When a memory-measuring cache evicts entries much more aggressively than expected, suspect that the measurement is overcounting. Specifically look for deeply shared singleton objects (schema metadata, type instances, function registries) being counted multiple times by a `measureDeep` traversal that has no cycle or exclusion awareness. The presence of a hand-rolled `Measurable*` interface is a warning sign that the underlying measurement library lacked proper exclusion support.

---

## Test

No test added.
