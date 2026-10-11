# Bug 0393: Streaming Repair Metrics Counted at Prepare Time Instead of Transfer Time

**Commit**: c4e4e9388b · **Ticket**: CASSANDRA-16190
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamSession.java
**Category**: Logic error in condition

---

## Root Cause

`StreamingMetrics.totalOutgoingRepairBytes` and `totalOutgoingRepairSSTables` were incremented inside `StreamSession.prepare()`, using pre-computed totals from `StreamTransferTask.getTotalSize()` and `getTotalNumberOfFiles()` before any data had actually been sent. This meant the metrics fired at negotiation time rather than at actual transfer time, making their values inaccurate (they could over-count if preparation succeeded but streaming later failed, or mis-count the true bytes-on-wire). The fix removes the early accounting from `prepare()` and instead increments these counters inside `StreamSession.sendComponents()` at the moment each file header is actually sent, using the real estimated size of the outgoing stream.

### Before / After

```java
// BEFORE (in prepare())
long totalBytesToStream = 0;
long totalSSTablesStreamed = 0;
for (StreamTransferTask task : transfers.values())
{
    totalBytesToStream += task.getTotalSize();
    totalSSTablesStreamed += task.getTotalNumberOfFiles();
    prepare.summaries.add(task.getSummary());
}
if (StreamOperation.REPAIR == getStreamOperation())
{
    StreamingMetrics.totalOutgoingRepairBytes.inc(totalBytesToStream);
    StreamingMetrics.totalOutgoingRepairSSTables.inc(totalSSTablesStreamed);
}
```

```java
// AFTER (in sendComponents(), per-file)
long headerSize = message.stream.getEstimatedSize();
StreamingMetrics.totalOutgoingBytes.inc(headerSize);
metrics.outgoingBytes.inc(headerSize);
if (StreamOperation.REPAIR == getStreamOperation())
{
    StreamingMetrics.totalOutgoingRepairBytes.inc(headerSize);
    StreamingMetrics.totalOutgoingRepairSSTables.inc(message.stream.getNumFiles());
}
```

---

## How to Spot

When metrics are aggregated eagerly at a planning/negotiation stage rather than at the actual I/O stage, they can drift from reality if the operation fails partway through or if the planned size differs from the wire size. During review, verify that throughput/byte metrics are updated at the point data crosses the wire, not at the point the plan is constructed.

---

## Test

Expanded `StreamingMetricsTest` adds repair and rebuild scenarios across a 3-node cluster, asserting that `totalOutgoingRepairBytes` and `totalOutgoingRepairSSTables` are non-zero after repair but remain zero after rebuild, and that per-peer `outgoingBytes` equals the corresponding `incomingBytes` on the receiving node.
