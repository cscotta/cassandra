# Bug 0235: BulkLoader Crashes Calling getPreferredIP() Without System Keyspace Access

**Commit**: 853ae8c840 · **Ticket**: CASSANDRA-18370
**Subsystem**: src/java/org/apache/cassandra/tools/BulkLoadConnectionFactory.java, streaming
**Category**: Logic error in condition

---

## Root Cause

`NettyStreamingMessageSender` was unconditionally calling `template.connectTo()` before submitting a `FileStreamTask`, which in turn called `SystemKeyspace.getPreferredIP()`. This method queries the local `system.peers_v2` table — a resource that is only available to a fully-initialised daemon, not to command-line tools. When `sstableloader` (BulkLoader) uses the streaming subsystem it runs in a tool context where `DatabaseDescriptor.isDaemonInitialized()` is `false`, causing an illegal state exception (enforced by the new `Preconditions.checkState` guard) or silently returning wrong data.

### Before / After

```java
// BEFORE — always pre-loads the preferred IP
OutboundConnectionSettings templateWithConnectTo =
    template.withConnectTo(template.connectTo());
fileTransferExecutor.submit(new FileStreamTask(..., templateWithConnectTo));
```

```java
// AFTER — skip preferred-IP lookup when the factory says it is not supported
OutboundConnectionSettings templateWithConnectTo =
    factory.supportsPreferredIp()
        ? template.withConnectTo(template.connectTo())
        : template;
fileTransferExecutor.submit(new FileStreamTask(..., templateWithConnectTo));
```

---

## How to Spot

Any call to a `SystemKeyspace` helper inside a shared utility that is also used by CLI tools needs a guard on `DatabaseDescriptor.isDaemonInitialized()`. Adding a `supportsPreferredIp()` capability method to the factory interface lets the caller opt-out without coupling the shared streaming code to tool-specific checks. Reviewing usages of `SystemKeyspace.getPreferredIP()` and confirming they are all guarded is the key review heuristic.

---

## Test

No dedicated test added; the regression is caught at the tool level by verifying `sstableloader` can complete streaming without an `IllegalStateException`.
