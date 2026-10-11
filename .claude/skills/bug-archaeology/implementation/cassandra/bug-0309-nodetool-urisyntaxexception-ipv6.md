# Bug 0309: nodetool URISyntaxException with IPv6 Host on Updated Java

**Commit**: 61d4cccb0b · **Ticket**: CASSANDRA-17581
**Subsystem**: src/java/org/apache/cassandra/tools/NodeProbe.java
**Category**: Wrong constant / default value

---

## Root Cause

`NodeProbe` built the JMX service URL using a format string that always wrapped the host in square brackets (`[%s]`). When the host is an IPv6 address, this is correct. However, newer Java versions became stricter about URI syntax and rejected the resulting URL when `%s` was a plain hostname or IPv4 address, because those do not need square brackets and the brackets are not valid URI characters in that context. The fix inverts the logic: the format string uses a plain `%s`, and the square brackets are added conditionally only when the host string contains a colon (i.e., is an IPv6 address).

### Before / After

```java
// BEFORE – brackets always applied, breaks IPv4/hostname on newer JDK
private static final String fmtUrl = "service:jmx:rmi:///jndi/rmi://[%s]:%d/jmxrmi";
```

```java
// AFTER – brackets applied only for IPv6 addresses
private static final String fmtUrl = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi";

// In connect():
String host = this.host;
if (host.contains(":"))
    host = "[" + host + "]";
JMXServiceURL jmxUrl = new JMXServiceURL(String.format(fmtUrl, host, port));
```

---

## How to Spot

Format strings that embed network addresses should not hardcode brackets for all cases; the brackets are required only for IPv6 literals in URI/URL contexts. During review of any code that constructs JMX or HTTP URLs from a user-supplied host string, verify that IPv4 addresses and hostnames are not wrapped in brackets. A test matrix with `127.0.0.1`, `::1`, and `localhost` as host values will catch this on any JDK.

---

## Test

No test added; the change is a one-line format string fix with conditional bracket logic in `connect()`.
