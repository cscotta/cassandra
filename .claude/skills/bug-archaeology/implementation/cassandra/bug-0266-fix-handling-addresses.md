# Bug 0266: StorageService.getNativeaddress Produces Unparseable IPv6 Address String

**Commit**: 3bdd2caa22 · **Ticket**: CASSANDRA-17945
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`StorageService.getNativeaddress` built the native address string by simple string concatenation of the raw IP text with a colon and port number. For IPv4 this yields `1.2.3.4:9042` which is unambiguous, but for IPv6 it yields a string like `0:0:0:0:0:0:5a:3:9042` which cannot be parsed as an `InetSocketAddress` because the port is indistinguishable from the last group of the address. The fix routes both the `RPC_ADDRESS` gossip case and the fallback case through `InetAddressAndPort.getByNameOverrideDefaults`, which brackets IPv6 addresses in RFC-2732 format, producing `[0:0:0:0:0:0:5a:3]:9042`.

### Before / After

```java
// BEFORE – naked string concat, breaks IPv6
return endpoint.address.getHostAddress() + ":" + DatabaseDescriptor.getNativeTransportPort();
```

```java
// AFTER – InetAddressAndPort handles bracketing
InetAddressAndPort address = InetAddressAndPort.getByNameOverrideDefaults(ipAddress,
        DatabaseDescriptor.getNativeTransportPort());
return address.getHostAddress(withPort);
```

---

## How to Spot

Any place that assembles an `host:port` string by concatenation rather than through an `InetAddressAndPort` (or equivalent) abstraction is a latent IPv6 bug. Review code that mixes address strings obtained from gossip application-state (which may contain bare IPv6 colons) with manual port appending.

---

## Test

`StorageServiceServerTest.testGetNativeAddressIPV6` checks that both the gossip `RPC_ADDRESS` path and the fallback path produce RFC-2732-formatted strings for IPv6 endpoints.
