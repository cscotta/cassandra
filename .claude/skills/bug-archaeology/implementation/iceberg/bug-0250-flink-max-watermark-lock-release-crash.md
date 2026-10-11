# Bug 0250: Flink LockRemoverOperator sends lock release event on MAX_WATERMARK, and IcebergSink crashes when no lock type is configured

**Commit**: afb58aa341d055a6bef81a54d1e9e20d07b7e075 · **Ticket**: #15562

## Detection Rule

When an operator processes a special sentinel watermark value (such as `Watermark.MAX_WATERMARK`) by forwarding it to a coordinator, verify that the sentinel is explicitly excluded from normal event dispatch, since MAX_WATERMARK signals pipeline termination and coordinator-side handlers may not tolerate it as a legitimate event payload.

## Code Smell

A `processWatermark` override that unconditionally calls `operatorEventGateway.sendEventToCoordinator(new LockReleaseEvent(..., mark.getTimestamp()))` for every watermark, including `Watermark.MAX_WATERMARK`, without a guard for the terminal sentinel value. Separately, a code path that calls `LockFactoryBuilder.build(lockConfig, ...)` without first checking whether `lockConfig.lockType()` is non-empty.

## Tags

- scope: distributed-systems
- category: missing-null-check
- code-pattern: sentinel-not-handled
- severity: crash
- subsystem: flink/maintenance/lock-remover, flink/sink/iceberg-sink

## Root Cause

`LockRemoverOperator.processWatermark` forwarded every watermark timestamp to the coordinator as a `LockReleaseEvent`, including Flink's `MAX_WATERMARK` (Long.MAX_VALUE), which is a pipeline-termination sentinel. The coordinator was not designed to handle this value and could crash or deadlock. Additionally, `IcebergSink` always called `LockFactoryBuilder.build(lockConfig, table.name())` regardless of whether a lock type was configured, throwing a `NullPointerException` or misconfiguration error when users omit lock configuration. The fix guards both paths: skip the coordinator event on MAX_WATERMARK, and only build the lock factory when `lockConfig.lockType()` is non-empty.

## Example

```java
// BEFORE
@Override
public void processWatermark(Watermark mark) throws Exception {
  operatorEventGateway.sendEventToCoordinator(
      new LockReleaseEvent(tableName, mark.getTimestamp()));
  super.processWatermark(mark);
}
```

```java
// AFTER
@Override
public void processWatermark(Watermark mark) throws Exception {
  if (Watermark.MAX_WATERMARK.getTimestamp() != mark.getTimestamp()) {
    operatorEventGateway.sendEventToCoordinator(
        new LockReleaseEvent(tableName, mark.getTimestamp()));
  }
  super.processWatermark(mark);
}
```
