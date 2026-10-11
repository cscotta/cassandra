# Bug 2182: HintedHandOff Thread Pool Registered Under Wrong JMX Path

**Commit**: 5467fb52f8 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/HintedHandOffManager.java
**Category**: Wrong constant / default value

---

## Root Cause

The `HintedHandoff` executor service was registered in JMX under the pool-name `"HintedHandoff"` as its MBean path prefix, but the convention for internal executor pools is to register them under the `"internal"` JMX domain so they appear alongside other internal thread pools in monitoring tools. Using `"HintedHandoff"` as the JMX path caused the pool to be invisible in standard dashboards and monitoring configurations that enumerate internal pools.

### Before / After

```java
// BEFORE
new NamedThreadFactory("HintedHandoff", Thread.MIN_PRIORITY), "HintedHandoff");
```

```java
// AFTER
new NamedThreadFactory("HintedHandoff", Thread.MIN_PRIORITY), "internal");
```

---

## How to Spot

When adding or reviewing executor service construction that includes a JMX registration path argument, verify the path matches the established convention for the pool's type (e.g., `"internal"` for internal background pools). Using the pool's descriptive name as the JMX domain is a common mistake.

---

## Test

No test added.
