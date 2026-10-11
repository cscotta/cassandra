# Bug 2326: Schema migration blocks migration stage with synchronous RPC, causing deadlock

**Commit**: 2247054af8 · **Ticket**: CASSANDRA-3882

## Detection Rule

When code on a dedicated processing stage (thread pool) performs a blocking synchronous RPC call with a retry loop, check whether the response to that RPC also needs to be processed on the same stage — a deadlock results if the stage is single-threaded.

## Code Smell

A while-retry loop calling `IAsyncResult iar = MessagingService.instance().sendRR(...)` followed by `iar.get(timeout, ...)` inside a `Runnable` dispatched to the migration stage — blocking a migration-stage thread waiting for a response that must be processed by a migration-stage thread.

## Tags

- scope: distributed-systems
- category: deadlock
- code-pattern: blocking-rpc-on-single-threaded-stage
- severity: hang
- subsystem: schema-metadata

## Root Cause

`MigrationManager` fetched remote schema by sending a request synchronously and blocking for the reply inside the migration stage. If the migration stage had only one thread (which is typical), that thread was blocked waiting for the reply, and the reply handler needed to run on the same stage to process the schema update — creating a classic deadlock. The fix converts the call to use an async callback (`IAsyncCallback`) so the migration thread is not blocked. The callback runs on the network-receive thread, processes the schema merge inline, and the migration stage thread is freed immediately.

## Example

```java
// BEFORE: blocking RPC on migration stage — deadlock
int retries = 0;
while (retries < MIGRATION_REQUEST_RETRIES)
{
    IAsyncResult iar = MessagingService.instance().sendRR(message, endpoint);
    try
    {
        byte[] reply = iar.get(DatabaseDescriptor.getRpcTimeout(), TimeUnit.MILLISECONDS);
        DefsTable.mergeRemoteSchema(reply, message.getVersion());
        return;
    }
    catch (TimeoutException e) { retries++; }
}
```

```java
// AFTER: async callback — migration stage thread not blocked
IAsyncCallback cb = new IAsyncCallback() {
    public void response(Message message) {
        DefsTable.mergeRemoteSchema(message.getMessageBody(), message.getVersion());
    }
    public boolean isLatencyForSnitch() { return false; }
};
MessagingService.instance().sendRR(message, endpoint, cb);
```
