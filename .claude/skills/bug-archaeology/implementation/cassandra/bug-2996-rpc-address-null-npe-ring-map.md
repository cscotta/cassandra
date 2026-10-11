# Bug 2996: NPE in getRangeToRpcaddressMap when rpc_address is null

**Commit**: bc756792aa · **Ticket**: CASSANDRA-2996

## Detection Rule
When `DatabaseDescriptor.getRpcAddress()` can return null (unconfigured), callers must either null-check the result or ensure the field is initialised to a default value in `DatabaseDescriptor` itself rather than at the call site.

## Code Smell
`rpcaddrs.add(DatabaseDescriptor.getRpcAddress().toString())` without a null check will throw NPE when `rpc_address` is not set in cassandra.yaml and the field has not been defaulted.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: optional-config-null-dereference
- severity: crash
- subsystem: gossip

## Root Cause
`StorageService.getRangeToRpcaddressMap()` called `DatabaseDescriptor.getRpcAddress()` and assumed the result was non-null. `DatabaseDescriptor` did not initialise `rpcAddress` when `rpc_address` was absent from the config, leaving it null. The fix initialises `rpcAddress` to `FBUtilities.getLocalAddress()` inside `DatabaseDescriptor` when no value is configured, and removes the duplicated null-handling in `AbstractCassandraDaemon` and `StorageService`.

## Example
```java
// BEFORE (StorageService) — NPE when rpcAddress is null
rpcaddrs.add(DatabaseDescriptor.getRpcAddress().toString());

// BEFORE (DatabaseDescriptor) — no default set
if (conf.rpc_address != null) {
    rpcAddress = InetAddress.getByName(conf.rpc_address);
}
// (else rpcAddress stays null)
```
```java
// AFTER (DatabaseDescriptor)
if (conf.rpc_address != null) {
    rpcAddress = InetAddress.getByName(conf.rpc_address);
} else {
    rpcAddress = FBUtilities.getLocalAddress();
}
```
