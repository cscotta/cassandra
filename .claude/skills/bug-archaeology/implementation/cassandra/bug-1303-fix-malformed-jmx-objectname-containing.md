# Bug 1303: Fix Malformed JMX ObjectName Containing IPv6 Addresses

**Commit**: 495ae9c7e7 · **Ticket**: CASSANDRA-9027
**Subsystem**: metrics/ConnectionMetrics.java, metrics/HintedHandoffMetrics.java, metrics/StreamingMetrics.java
**Category**: Wrong constant / default value

---

## Root Cause

JMX `ObjectName` keys have a restricted character set; colons (`:`) are used as delimiters and are not allowed in key-value components. IPv6 addresses naturally contain colons (e.g. `fe80::1`), so using a raw `getHostAddress()` result in a JMX metric name caused a `MalformedObjectNameException` at runtime on IPv6-only or dual-stack nodes. The fix replaces each colon with a period in all three metrics classes. Additionally, `replaceAll(":", ".")` (which treats the first argument as a regex) was replaced with `replace(':', '.')` (literal character replacement), removing a subtle correctness issue since `:` is a special character in some regex contexts.

### Before / After

```java
// BEFORE — raw IPv6 address; colons break JMX ObjectName
address = ip.getHostAddress().replaceAll(":", ".");
```

```java
// AFTER — colons replaced with periods; uses literal char replace
address = ip.getHostAddress().replace(':', '.');
```

---

## How to Spot

Any time a network address string is embedded into a JMX metric name, MBean ObjectName, or similar structured identifier, it must be sanitized. Colons, commas, equals signs, and asterisks are all reserved in JMX ObjectName syntax. Search for `getHostAddress()` or `toString()` on `InetAddress` objects that feed directly into name factories, metric registries, or string-keyed maps used as identifiers.

---

## Test

No test added.
