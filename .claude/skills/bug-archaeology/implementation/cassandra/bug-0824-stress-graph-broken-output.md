# Bug 0824: cassandra-stress graph option broken output

**Commit**: f42e235b1e · **Ticket**: CASSANDRA-12237
**Subsystem**: cassandra-stress / graph reporting
**Category**: Logic error in condition

---

## Root Cause
The `StressGraph` parser converted key names from the stress log into JSON without lower-casing them, but the graphing JavaScript expected all-lowercase keys. Two additional problems compounded this: (1) an empty JSON object (produced from the trailing newline of the log) was always appended to the stats list, and (2) the `StressAction.warmup()` method created a dummy `PrintStream` that discarded all output and was then threaded everywhere instead of a no-op abstraction — preventing the graph feature from capturing warmup output and tangling `PrintStream` throughout the API. The fix lower-cases map keys during parsing, skips empty JSON objects, and introduces a `ResultLogger` interface with a proper `NOOP` singleton so no-op logging is explicit.

### Before / After
```java
// BEFORE — key names not lowercased, empty stats appended
json.put(parts[0].trim(), parts[1].trim());
...
stats.add(json);

// AFTER — keys lowercased, empty object skipped
json.put(parts[0].trim().toLowerCase(), parts[1].trim());
...
if (json.size() != 0) stats.add(json);
```

---

## How to Spot
When a method transforms data for a downstream consumer (especially a scripting layer), watch for any case-sensitivity mismatch between the producer and the consumer. In graph/charting pipelines a single upper-case key silently produces no series on the chart without any error. Similarly, any method that appends to a collection inside a loop should guard against appending empty/default-valued entries when the loop exits normally.

---

## Test
A new `MultiResultLoggerTest` is added covering the `MultiResultLogger` delegation to multiple streams, but no test directly exercises the graph-key-casing or empty-JSON-guard fixes.
