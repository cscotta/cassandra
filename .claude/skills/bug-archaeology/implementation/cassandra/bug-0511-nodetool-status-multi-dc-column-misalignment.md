# Bug 0511: nodetool status Multi-DC Output Has Misaligned Columns Per Datacenter

**Commit**: 7b7212338c · **Ticket**: CASSANDRA-15305
**Subsystem**: `src/java/org/apache/cassandra/tools/nodetool`
**Category**: Logic error in condition

---

## Root Cause

`Status` used a single shared `TableBuilder` instance for all datacenters, which computed column widths only from the data it happened to see last. When printing, each datacenter's table was rendered with widths derived from the global builder rather than widths normalised across all datacenters, so columns in different DCs could be differently padded. Compounding this, the code had a separate code path for `--with-ports` vs. without, duplicating the rendering logic and creating divergence. Additionally, `HostStatWithPort` keyed the per-DC multimap on `stat.endpoint` (an `InetAddressAndPort`) rather than `stat.endpointWithPort`, which meant the port-aware key was lost when `--with-ports` was active on `DescribeCluster` too.

### Before / After

```java
// BEFORE: single shared builder, separate code paths
TableBuilder tableBuilder = new TableBuilder("  ");
if (printPort) {
    // ... duplicate DC loop using hostToTokens.put(stat.endpoint, stat)
    tableBuilder.printTo(System.out);
} else {
    // ... duplicate DC loop
    tableBuilder.printTo(System.out);
}
```

```java
// AFTER: shared-width coordinator collects one builder per DC
TableBuilder.SharedTable sharedTable = new TableBuilder.SharedTable("  ");
for (Map.Entry<String, SetHostStatWithPort> dc : dcs.entrySet()) {
    TableBuilder tableBuilder = sharedTable.next();
    hostToTokens.put(stat.ipOrDns(printPort), stat); // unified key
}
// SharedTable.complete() normalises widths across all per-DC builders
Iterator<TableBuilder> results = sharedTable.complete().iterator();
```

---

## How to Spot

When the same tabular data is rendered across multiple logical groups (DCs, racks, regions), using a single `TableBuilder` or computing column widths per group independently will produce misaligned output unless widths are explicitly normalised across all groups before printing. The safe pattern is to collect all rows first, then perform a single width-normalisation pass before any output is emitted — exactly the `SharedTable.complete()` approach introduced here.

---

## Test

No test added.
