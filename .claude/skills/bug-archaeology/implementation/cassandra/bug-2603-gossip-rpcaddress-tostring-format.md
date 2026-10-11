# Bug 2603: InetAddress.toString() includes hostname prefix in gossip RPC address

**Commit**: ba7c584758 · **Ticket**: n/a

## Detection Rule
When serialising an `InetAddress` to a string for storage or transmission, use `getHostAddress()` rather than `toString()` to avoid the `hostname/address` format that `toString()` produces.

## Code Smell
`new VersionedValue(endpoint.toString())` stores the address as `hostname/1.2.3.4` instead of just `1.2.3.4`, causing downstream parsers that expect a plain IP string to fail.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: inetaddress-tostring-includes-hostname
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
`VersionedValue.Factory.rpcaddress()` called `endpoint.toString()` which in Java produces `"hostname/ip"` format. Gossip recipients and tools that later read the `RPC_ADDRESS` application state expected a bare IP address string and would either fail to parse or produce incorrect ring descriptions. The fix uses `endpoint.getHostAddress()` which returns only the dotted-decimal IP.

## Example
```java
// BEFORE
public VersionedValue rpcaddress(InetAddress endpoint) {
    return new VersionedValue(endpoint.toString()); // yields "hostname/1.2.3.4"
}
```
```java
// AFTER
public VersionedValue rpcaddress(InetAddress endpoint) {
    return new VersionedValue(endpoint.getHostAddress()); // yields "1.2.3.4"
}
```
