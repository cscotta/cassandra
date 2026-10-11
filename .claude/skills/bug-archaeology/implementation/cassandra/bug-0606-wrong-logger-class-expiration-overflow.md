# Bug 0606: Logger Initialized with Wrong Class in ExpirationDateOverflowHandling

**Commit**: ddaaf924e9 · **Ticket**: n/a
**Subsystem**: `src/java/org/apache/cassandra/db/ExpirationDateOverflowHandling.java`
**Category**: Wrong constant / default value

---

## Root Cause

`ExpirationDateOverflowHandling` passed `Attributes.class` to `LoggerFactory.getLogger` instead of `ExpirationDateOverflowHandling.class`. As a result, all log output from this class appeared under the `org.apache.cassandra.cql3.Attributes` logger name rather than `org.apache.cassandra.db.ExpirationDateOverflowHandling`. This made it impossible to selectively enable or filter logging for expiration overflow warnings using the correct class name in `logback.xml` or via JMX.

### Before / After

```java
// BEFORE — wrong class reference
private static final Logger logger = LoggerFactory.getLogger(Attributes.class);
```

```java
// AFTER — correct class reference
private static final Logger logger = LoggerFactory.getLogger(ExpirationDateOverflowHandling.class);
```

---

## How to Spot

When copying a logger declaration from another class, the class argument to `LoggerFactory.getLogger` is easily left as the source class. Automated checkstyle rules can flag logger declarations where the argument does not match the enclosing class. Code review should verify the class literal matches the file name.

---

## Test

No test added.
