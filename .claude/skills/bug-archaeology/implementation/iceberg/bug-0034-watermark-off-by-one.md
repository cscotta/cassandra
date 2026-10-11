# Bug 0034: Watermark emitted at split minimum timestamp instead of one below it

**Commit**: 8c217a35b0eb78f04b4127e38eee0683bb6f1931 · **Ticket**: #15884

## Detection Rule

When a watermark is derived from the minimum event time of a data batch, verify that the emitted watermark is `minTimestamp - 1`, not `minTimestamp`, because watermark W means all events with time <= W have already arrived.

## Code Smell

A watermark variable initialized to `0` (default `long`) and assigned directly from `timeExtractor.extractWatermark(split)` without subtracting 1, causing records whose event time equals the extracted value to be treated as late.

## Tags

- scope: distributed-systems
- category: off-by-one
- code-pattern: watermark-boundary-inclusive
- severity: silent-wrong-result
- subsystem: flink/source/reader

## Root Cause

The `WatermarkExtractorRecordEmitter` used the raw minimum timestamp of a split as the watermark value. By Flink's watermark contract, a watermark W declares that all events with `eventTime <= W` have arrived; records at exactly `W` are therefore considered on-time. Setting the watermark to `minSplitTs` rather than `minSplitTs - 1` caused records whose event time equalled the split's minimum timestamp to be counted as late and potentially dropped from windows. Additionally, the field was default-initialized to `0` instead of `Long.MIN_VALUE`, producing an incorrect initial watermark.

## Example

```java
// BEFORE
private long watermark;  // default 0, incorrect initial value

long newWatermark = timeExtractor.extractWatermark(split);
```

```java
// AFTER
private long watermark = Long.MIN_VALUE;

long extracted = timeExtractor.extractWatermark(split);
// Subtract 1: watermark W means all records with eventTime <= W have arrived
long newWatermark = extracted > Long.MIN_VALUE ? extracted - 1 : Long.MIN_VALUE;
```
