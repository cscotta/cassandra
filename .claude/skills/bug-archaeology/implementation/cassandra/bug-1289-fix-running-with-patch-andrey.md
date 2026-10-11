# Bug 1289: Fix Running with java.net.preferIPv6Addresses=true

**Commit**: a5bae1f5df · **Ticket**: CASSANDRA-9137
**Subsystem**: src/java/org/apache/cassandra/service/CassandraDaemon.java
**Category**: Wrong constant / default value

---

## Root Cause

When JMX is configured to listen on the loopback interface, `CassandraDaemon` hardcoded the string `"127.0.0.1"` as the `java.rmi.server.hostname` property. On a JVM started with `-Djava.net.preferIPv6Addresses=true`, the JVM's loopback address resolves to `::1` (IPv6), not `127.0.0.1`. Passing the IPv4 literal caused the RMI server to advertise an address that clients following IPv6 preference could not reach, breaking JMX connectivity entirely in IPv6-preferred environments.

The fix replaces the hardcoded string with `InetAddress.getLoopbackAddress().getHostAddress()`, which returns the correct loopback address for the active address family.

### Before / After

```java
// BEFORE
System.setProperty("java.rmi.server.hostname", "127.0.0.1");
```

```java
// AFTER
System.setProperty("java.rmi.server.hostname", InetAddress.getLoopbackAddress().getHostAddress());
```

---

## How to Spot

Hardcoded IPv4 address literals (`"127.0.0.1"`, `"0.0.0.0"`) in networking code are a reliable indicator of IPv6 incompatibility. Reviewers should flag any literal IP address string and ask whether the system property `java.net.preferIPv6Addresses` or dual-stack configurations could change the expected address family. The standard Java idiom for the loopback is `InetAddress.getLoopbackAddress()`, not a string constant.

---

## Test

No test added.
