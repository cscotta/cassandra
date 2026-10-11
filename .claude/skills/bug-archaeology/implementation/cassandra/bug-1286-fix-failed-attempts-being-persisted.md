# Bug 1286: Local JMX Binds to Hardcoded IPv4 Loopback, Breaking IPv6-Only Hosts

**Commit**: ee9bd549e8 · **Ticket**: CASSANDRA-9180
**Subsystem**: src/java/org/apache/cassandra/service/CassandraDaemon.java
**Category**: Wrong constant / default value

---

## Root Cause

When Cassandra starts a local (non-remote) JMX connector, it sets `java.rmi.server.hostname` to the literal string `"127.0.0.1"`. On an IPv6-only host or when the JVM's loopback address is `::1`, the hardcoded IPv4 address causes the RMI registry to bind to the wrong interface, making local JMX connections fail. Using `InetAddress.getLoopbackAddress().getHostAddress()` instead lets the JVM resolve the correct loopback address for whatever IP stack is active.

### Before / After

```java
// BEFORE — hardcoded IPv4 loopback breaks IPv6-only environments
System.setProperty("java.rmi.server.hostname","127.0.0.1");
```

```java
// AFTER — JVM resolves the correct loopback address for the active IP stack
System.setProperty("java.rmi.server.hostname", InetAddress.getLoopbackAddress().getHostAddress());
```

---

## How to Spot

Search for string literals like `"127.0.0.1"` or `"localhost"` used as network addresses in code that also handles `InetAddress` objects. These hardcoded strings are IPv4-specific and break transparently when the node runs in IPv6-only mode. Prefer the `InetAddress` API (`getLoopbackAddress()`, `getByName("localhost")`) so the JVM handles address family selection.

---

## Test

No test added.
