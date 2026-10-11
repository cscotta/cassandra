# Bug 0349: WaitLatencies Missing from `nodetool tpstats -F` JSON/YAML Output

**Commit**: 636ab42bd5 · **Ticket**: CASSANDRA-16938
**Subsystem**: src/java/org/apache/cassandra/tools/nodetool/stats/TpStatsHolder.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

Two compounding defects caused `WaitLatencies` to be absent or garbled in structured output. First, in `TpStatsHolder`, the wait-latency array was built by streaming `Double[]` values from `probe.metricPercentilesAsArray()` through `.map(D -> D.toString()).toArray()`, which returns a plain `Object[]` (not `String[]`) due to type erasure — the cast to `String[]` fails silently and the resulting array stores `Object` references that serializers do not know how to render as numbers. Second, `StatsPrinter.JsonPrinter` used `org.json.simple.JSONObject` which cannot serialize Java arrays or typed numeric values as proper JSON; switching to Jackson's `ObjectMapper` with pretty-printing fixed the serialization. The combined effect was that the `WaitLatencies` section was either empty or omitted entirely in JSON and YAML output.

### Before / After

```java
// BEFORE (TpStatsHolder)
HashMap<String, String[]> waitLatencies = new HashMap<>();
// ...
String[] strValues = (String[]) Arrays.stream(probe.metricPercentilesAsArray(...))
                                      .map(D -> D.toString())
                                      .toArray();   // returns Object[], cast fails at runtime
waitLatencies.put(key, strValues);
```

```java
// AFTER
HashMap<String, Double[]> waitLatencies = new HashMap<>();
// ...
waitLatencies.put(key, probe.metricPercentilesAsArray(probe.getMessagingQueueWaitMetrics(key)));
```

---

## How to Spot

When converting a typed stream to an array via `.toArray()` without a constructor argument, the result is always `Object[]` regardless of the stream element type — a cast to a typed array compiles but throws `ClassCastException` at runtime (or silently corrupts the reference in some paths). Additionally, `org.json.simple` does not serialize Java arrays or boxed primitives faithfully; prefer Jackson for any structured output that includes arrays of numeric values.

---

## Test

Adds `StatsPrinterTest` unit tests covering JSON and YAML rendering of all primitive, array, and list types; also extends `NodeToolTPStatsTest.testFormatArg` to assert that `WaitLatencies` appears as a non-empty section in both JSON and YAML output.
