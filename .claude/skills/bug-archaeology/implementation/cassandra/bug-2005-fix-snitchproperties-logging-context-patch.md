# Bug 2005: SnitchProperties Logger Used Wrong Class as Logging Context

**Commit**: 456e91d182 · **Ticket**: CASSANDRA-5602
**Subsystem**: src/java/org/apache/cassandra/locator/SnitchProperties.java
**Category**: Wrong constant / default value

---

## Root Cause

`SnitchProperties` declared its `Logger` using `GossipingPropertyFileSnitch.class` as the logging context instead of `SnitchProperties.class`. As a result, all log output from `SnitchProperties` appeared under the wrong logger name, making it impossible to filter or identify log messages originating from this class. Log configuration targeting `SnitchProperties` would have no effect.

### Before / After

```java
// BEFORE
private static final Logger logger = LoggerFactory.getLogger(GossipingPropertyFileSnitch.class);
```

```java
// AFTER
private static final Logger logger = LoggerFactory.getLogger(SnitchProperties.class);
```

---

## How to Spot

Copy-paste errors in logger declarations are common. During review, verify that every `LoggerFactory.getLogger(X.class)` uses the enclosing class `X`, not a neighboring class. Static analysis tools can detect this pattern automatically.

---

## Test

No test added.
