# Bug 1218: Deprecated Repair JMX API Ignores Primary-Range and Local-DC Options

**Commit**: cab33a6095 · **Ticket**: CASSANDRA-9570
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Silent skip on non-match

---

## Root Cause

The older (deprecated) overloads of `StorageService.repair()` accepted a `primaryRange` boolean and optional `hosts`/`dataCenters` lists but never populated `options.getRanges()`. In the newer API path that code was responsible for calling `getPrimaryRanges()`, `getPrimaryRangesWithinDC()`, or `getLocalRanges()` based on those flags. Because those calls were simply absent in the deprecated overload, every repair triggered via the legacy JMX entry point silently repaired no token ranges at all — the repair session started and completed without error but produced no actual data validation.

### Before / After

```java
// BEFORE — options.getRanges() never populated; deprecated overload exits
// here with an empty range set
if (columnFamilies != null)
{
    for (String columnFamily : columnFamilies)
```

```java
// AFTER — range logic inserted before column-family processing
if (primaryRange)
{
    if (options.getDataCenters().isEmpty() && options.getHosts().isEmpty())
        options.getRanges().addAll(getPrimaryRanges(keyspace));
    else if (options.getDataCenters().size() == 1 &&
             options.getDataCenters().contains(DatabaseDescriptor.getLocalDataCenter()))
        options.getRanges().addAll(getPrimaryRangesWithinDC(keyspace));
    else
        throw new IllegalArgumentException(
            "You need to run primary range repair on all nodes in the cluster.");
}
else
{
    options.getRanges().addAll(getLocalRanges(keyspace));
}
if (columnFamilies != null)
{
    for (String columnFamily : columnFamilies)
```

---

## How to Spot

When a new API path is introduced and the old deprecated path delegates to a shared `RepairOption` builder, check that every option the old path accepted is faithfully forwarded into the builder. A silent "no-op" is especially dangerous here: the operation completes without error but accomplishes nothing. Integration tests should exercise the deprecated JMX entry points directly and verify that a non-empty range set is actually repaired.

---

## Test

No test added.
