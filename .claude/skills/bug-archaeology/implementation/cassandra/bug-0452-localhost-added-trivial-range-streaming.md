# Bug 0452: Localhost Added as Streaming Source for Trivial Ranges

**Commit**: 5ec3bae22b · **Ticket**: CASSANDRA-16099
**Subsystem**: `src/java/org/apache/cassandra/dht/RangeFetchMapCalculator.java`
**Category**: Logic error in condition

---

## Root Cause

`RangeFetchMapCalculator` computes which endpoints to stream from for each token range. For "trivial" ranges (ranges the local node already owns), the code iterated over candidate replicas and, for each one that passed the filter, stored `fetchMap.put(replica.endpoint(), trivialRange)` and broke. The filter (`passFilters`) is designed to allow localhost through as a signal that the local node already covers the range — but the intent is only to *count* it as satisfied, not to stream from it. When the local node happened to be the first candidate that passed filters, it was added to the fetch map, causing a self-streaming attempt.

### Before / After

```java
// BEFORE
if (passFilters(replica, localDCCheck))
{
    fetchMap.put(replica.endpoint(), trivialRange);
    added = true;
    break;
}
```

```java
// AFTER
if (passFilters(replica, localDCCheck))
{
    added = true;
    if (replica.isSelf())
        continue; // count as covered but don't stream from localhost
    fetchMap.put(replica.endpoint(), trivialRange);
    break;
}
```

---

## How to Spot

When a filter's documented purpose is "passes if endpoint is acceptable as a data source — including the local node for accounting purposes", code that immediately uses a passing result for a fetch/stream action must also verify that the endpoint is actually remote. The `isSelf()` / `isLocal()` check needs to appear at the action site, not only in the filter.

---

## Test

`RangeFetchMapCalculatorTest.testTrivalRangeLocalHostStreaming` creates trivial ranges where localhost appears as a candidate source and asserts that `validateRangeFetchMap` succeeds (i.e., localhost is not inserted into the fetch map).
