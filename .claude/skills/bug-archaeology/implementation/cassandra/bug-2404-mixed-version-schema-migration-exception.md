# Bug 2404: Schema migration throws exception in mixed-version cluster when old node sends schema

**Commit**: 5888fcd6af · **Ticket**: n/a

## Detection Rule

When inter-node message handling throws an exception for messages from older-version peers rather than silently ignoring them, it can destabilize a rolling upgrade; instead log and return early.

## Code Smell

`deserializeMigrationMessage` throws `IOException("Can't accept schema migrations from Cassandra versions previous to 1.1")` — exceptions thrown deep in message handling propagate to callers that may not expect them during a mixed-version rolling upgrade.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: exception-instead-of-guard-return
- severity: crash
- subsystem: schema-metadata

## Root Cause

During a rolling upgrade from 1.0 to 1.1, a 1.1 node receiving a schema migration message from a 1.0 node would throw an `IOException` inside `deserializeMigrationMessage`, causing unexpected failures. The fix moves the version gate to the entry points (`mergeRemoteSchema` and `rectifySchema`) as early-return guards, and also prevents 1.1 nodes from pushing schema mutations to pre-1.1 peers. The exception in `deserializeMigrationMessage` is removed since by that point the caller has already validated the version.

## Example

```java
// BEFORE — throws exception deep in handler
public static Collection<RowMutation> deserializeMigrationMessage(byte[] data, int version)
        throws IOException {
    if (version < MessagingService.VERSION_11)
        throw new IOException("Can't accept schema migrations ...");  // disrupts rolling upgrade
    ...
}
```

```java
// AFTER — silent guard at entry points
public static void mergeRemoteSchema(byte[] data, int version) throws ... {
    if (version < MessagingService.VERSION_11) {
        logger.error("Can't accept schema migrations ...");
        return;   // safe no-op
    }
    ...
}
public static void rectifySchema(UUID theirVersion, InetAddress endpoint) {
    if (Gossiper.instance.getVersion(endpoint) < MessagingService.VERSION_11)
        return;
    ...
}
```
