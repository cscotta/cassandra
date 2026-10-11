# Bug 0255: MAX_WATERMARK forwarded to coordinator as lock release event

**Commit**: b310cba27cefdc9c3ac0bd620dc36d1d1879ecb9 · **Ticket**: #15459

## Detection Rule

When a `processWatermark` override forwards every watermark to a coordinator or external system, verify that the Flink terminal sentinel `Watermark.MAX_WATERMARK` is explicitly excluded, as it signals job completion rather than a normal progress event.

## Code Smell

A `processWatermark` implementation that calls `sendEventToCoordinator(...)` unconditionally on every watermark, with no guard against `Watermark.MAX_WATERMARK.getTimestamp()`.

## Tags

- scope: distributed-systems
- category: wrong-filter-result
- code-pattern: sentinel-not-handled
- severity: crash
- subsystem: flink/maintenance/operator/LockRemoverOperator

## Root Cause

`LockRemoverOperator.processWatermark` sent a `LockReleaseEvent` to the coordinator for every watermark, including Flink's `MAX_WATERMARK` (Long.MAX_VALUE) which is emitted at job shutdown. The coordinator interprets this as a normal lock-release attempt at timestamp `Long.MAX_VALUE`, which can trigger undefined state in the coordinator's lock-management logic during teardown. The fix guards the send behind a timestamp inequality check.

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
