# Bug 1657: NPE in FailureDetector When Using -Dcassandra.fd_initial_value_ms

**Commit**: 7f019804ca · **Ticket**: CASSANDRA-6751
**Subsystem**: src/java/org/apache/cassandra/gms
**Category**: Missing null / bounds check

---

## Root Cause

`FailureDetector` declared its `static final Logger` field *after* the `static final INITIAL_VALUE` field, which is initialized by calling `getInitialValue()`. That method attempts to use `logger` (e.g., to log the parsed system property value). Because Java initializes static fields in declaration order, `logger` is still `null` when `getInitialValue()` runs, causing a NullPointerException during class initialization whenever `-Dcassandra.fd_initial_value_ms` is specified. Moving the `logger` declaration to the top of the class ensures it is initialized before any other static that depends on it.

### Before / After

```java
// BEFORE
public static final String MBEAN_NAME = "...";
private static final int SAMPLE_SIZE = 1000;
protected static final int INITIAL_VALUE = getInitialValue(); // uses logger -> NPE
public static final IFailureDetector instance = new FailureDetector();
private static final Logger logger = LoggerFactory.getLogger(FailureDetector.class);
```

```java
// AFTER
private static final Logger logger = LoggerFactory.getLogger(FailureDetector.class);
public static final String MBEAN_NAME = "...";
private static final int SAMPLE_SIZE = 1000;
protected static final int INITIAL_VALUE = getInitialValue(); // logger already set
public static final IFailureDetector instance = new FailureDetector();
```

---

## How to Spot

When reviewing classes with multiple `static final` fields, verify that any field whose initializer calls a helper method that references another static field is declared *after* that dependency. Linters that track static-initialization order (e.g., SpotBugs SI_INSTANCE_BEFORE_FINALS_ASSIGNED) flag this pattern.

---

## Test

No test added.
