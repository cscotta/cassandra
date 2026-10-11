# Bug 2613: StorageService getRangeToRpcaddressMap uses InetAddress.toString() for local address

**Commit**: b6ea29d251 · **Ticket**: n/a

## Detection Rule
When formatting a local node's RPC address for display or storage, use `getHostAddress()` rather than `toString()` to return only the IP string without the `hostname/` prefix.

## Code Smell
`rpcaddrs.add(DatabaseDescriptor.getRpcAddress().toString())` produces `"hostname/1.2.3.4"` instead of `"1.2.3.4"`, inconsistent with gossip-sourced addresses from remote nodes.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: inetaddress-tostring-includes-hostname
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
`StorageService.getRangeToRpcaddressMap()` formatted the local node's RPC address with `toString()` which returns `"hostname/ip"`. Remote addresses were taken from gossip state as plain IP strings. The inconsistency caused ring display tools to show a differently-formatted entry for the local node. The fix uses `getHostAddress()`.

## Example
```java
// BEFORE
rpcaddrs.add(DatabaseDescriptor.getRpcAddress().toString());
```
```java
// AFTER
rpcaddrs.add(DatabaseDescriptor.getRpcAddress().getHostAddress());
```
