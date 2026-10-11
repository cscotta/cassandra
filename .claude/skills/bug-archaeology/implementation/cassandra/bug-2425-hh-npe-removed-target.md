# Bug 2425: NPE in Hinted Handoff when target node has been removed

**Commit**: 333e7ace0a · **Ticket**: CASSANDRA-3677

## Detection Rule

When a token-to-endpoint lookup returns a nullable result, check that all call sites guard against null before using the returned address.

## Code Smell

`getEndpoint(token)` is called and its return value is passed immediately to another method without a null check; the tombstone case (removed endpoint) causes a NullPointerException.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: missing-null-check-after-lookup
- severity: crash
- subsystem: gossip

## Root Cause

During hinted handoff startup, `StorageService.getTokenMetadata().getEndpoint(token)` can return null when the corresponding node has since been decommissioned and its token record is now a tombstone. The code called `scheduleHintDelivery(target)` unconditionally, causing an NPE.

## Example

```java
// BEFORE
Token<?> token = StorageService.getPartitioner().getTokenFactory().fromByteArray(row.key.key);
InetAddress target = StorageService.instance.getTokenMetadata().getEndpoint(token);
scheduleHintDelivery(target); // NPE if token was removed

// AFTER
InetAddress target = StorageService.instance.getTokenMetadata().getEndpoint(token);
// token may have since been removed (in which case we have just read back a tombstone)
if (target != null)
    scheduleHintDelivery(target);
```
