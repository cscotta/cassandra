# Bug 1532: Fix Serialization of Command Ratios in cassandra-stress

**Commit**: 24f6f038ef · **Ticket**: CASSANDRA-7559
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/settings
**Category**: Wrong serialization / deserialization

---

## Root Cause

`OptionAnyProbabilities.ratios()` and `OptionEnumProbabilities.ratios()` returned `List<Pair<K,Double>>` where `Pair` came from Apache Commons Math. When the settings object was serialized (for stressd), the Commons Math `Pair` is not a standard `java.io.Serializable` container and caused serialization failures. The fix changes the return type to `Map<K, Double>` (a plain `HashMap`), eliminating the third-party `Pair` dependency from the serialized graph. Callers that need the `List<Pair>` form for `SampledOpDistributionFactory` convert locally just before use.

### Before / After

```java
// BEFORE
List<Pair<String, Double>> ratios()
{
    List<Pair<String, Double>> ratiosOut = new ArrayList<>();
    for (Map.Entry<String, Double> e : ratios.options.entrySet())
        ratiosOut.add(new Pair<String, Double>(e.getKey(), e.getValue()));
    return ratiosOut;
}
```

```java
// AFTER
Map<String, Double> ratios()
{
    return ratios.options;
}

// Conversion deferred to call site in SettingsCommandUser.getFactory()
final List<Pair<String,Double>> mathPairs = new ArrayList<>();
for (Map.Entry entry: ratios.entrySet())
    mathPairs.add(new Pair(entry.getKey(), entry.getValue()));
return new SampledOpDistributionFactory<String>(mathPairs, clustering) { ... };
```

---

## How to Spot

When a `Serializable` settings class holds fields whose declared types come from third-party libraries, verify those types are themselves serializable. Prefer standard JDK collection types (`Map`, `List`) as the surface API of serialized objects and convert to library-specific types at the last moment.

---

## Test

No test added.
