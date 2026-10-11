# Bug 1593: Fix Logging Context in CompressedStreamReader

**Commit**: cd9e4638fa · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/streaming/compress/CompressedStreamReader.java
**Category**: Wrong constant / default value

---

## Root Cause

`CompressedStreamReader` declared its logger using the parent class `StreamReader.class` as the logger name instead of its own class. This caused all log output from `CompressedStreamReader` to appear under the `StreamReader` category, making it impossible to configure or filter logging for the compressed streaming code path separately, and producing misleading log context when diagnosing issues.

### Before / After

```java
// BEFORE: wrong class used for logger
private static final Logger logger = LoggerFactory.getLogger(StreamReader.class);
```

```java
// AFTER: correct class reference
private static final Logger logger = LoggerFactory.getLogger(CompressedStreamReader.class);
```

---

## How to Spot

During review, check that every class passes its own class literal to `LoggerFactory.getLogger(...)`. Copy-paste errors when subclassing are common here; a simple grep for `getLogger(` will expose any mismatches between the enclosing class name and the argument.

---

## Test

No test added.
