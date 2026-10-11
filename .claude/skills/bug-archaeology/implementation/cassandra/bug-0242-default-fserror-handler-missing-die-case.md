# Bug 0242: DefaultFSErrorHandler Missing `die` Case in Disk-Failure Switch

**Commit**: be574fc34b · **Ticket**: CASSANDRA-18294
**Subsystem**: service/DefaultFSErrorHandler
**Category**: Logic error in condition

---

## Root Cause

`DefaultFSErrorHandler` handles file-system errors with a `switch` over `DiskFailurePolicy`. Both the `handleFSError` and `handleCorruptSSTable` methods had a `case stop_paranoid:` that stopped transports and gossip, but the `case die:` label was absent from both switch blocks. Because Java switch falls through from the matching case downward, the absence of `die` as its own case meant that a node configured with `disk_failure_policy: die` would fall through to a `default:` branch and take no action on disk errors, leaving the node running in a degraded state instead of stopping. Adding `case die:` immediately before `case stop_paranoid:` restores fall-through so both policies trigger the transport-stopping code.

### Before / After

```java
// BEFORE
switch (DatabaseDescriptor.getDiskFailurePolicy())
{
    case stop_paranoid:
        logger.error("Stopping transports...");
        ...
```

```java
// AFTER
switch (DatabaseDescriptor.getDiskFailurePolicy())
{
    case die:
    case stop_paranoid:
        logger.error("Stopping transports...");
        ...
```

---

## How to Spot

Any switch statement over an enum that has a `die`/`kill`/`halt` severity level should have a case label for that level — not just adjacent levels — unless the intent is to ignore it. When adding a new enum value or reviewing an existing handler, enumerate all enum constants and confirm each one maps to a case label or an explicit `default`. Missing cases on `die`-severity policies are particularly dangerous because the silent skip goes unnoticed until production.

---

## Test

New parameterized `DefaultFSErrorHandlerTest` verifies that `disk_failure_policy: die` stops gossip on both `FSReadError` and `CorruptSSTableException`. New `DiskFailurePolicyTest` exercises the full matrix of policies x exception types x startup state, verifying JVM-kill and gossip-stop expectations.
