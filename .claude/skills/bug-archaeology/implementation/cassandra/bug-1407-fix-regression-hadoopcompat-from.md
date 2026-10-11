# Bug 1407: Fix regression in HadoopCompat from CASSANDRA-7579

**Commit**: deff175a4e · **Ticket**: CASSANDRA-8292
**Subsystem**: src/java/org/apache/cassandra/hadoop/HadoopCompat.java
**Category**: Logic error in condition

---

## Root Cause

`HadoopCompat` uses reflection to locate `getCounter` on either `TaskAttemptContext` (Hadoop 2.x) or `TaskInputOutputContext` (Hadoop 1.x), with a try/catch fallback between the two. CASSANDRA-7579 added a call to `JVMStabilityInspector.inspectThrowable(e)` inside the catch block as part of a broad sweep to handle fatal JVM errors. However, the `Exception` caught here is a routine `NoSuchMethodException` or `ClassNotFoundException` from reflection — it is explicitly part of normal control flow for Hadoop version detection, not an unexpected error. Passing it to `JVMStabilityInspector` could trigger incorrect fatal-error handling or logging for an entirely benign reflection miss, and constituted a regression in the compatibility detection logic.

### Before / After

```java
// BEFORE
try {
    get_counter = Class.forName(PACKAGE + ".TaskAttemptContext")
                       .getMethod("getCounter", String.class, String.class);
} catch (Exception e) {
    JVMStabilityInspector.inspectThrowable(e);   // wrong: treats version-probe miss as fatal
    get_counter = Class.forName(PACKAGE + ".TaskInputOutputContext")
                       .getMethod("getCounter", String.class, String.class);
}
```

```java
// AFTER
try {
    get_counter = Class.forName(PACKAGE + ".TaskAttemptContext")
                       .getMethod("getCounter", String.class, String.class);
} catch (Exception e) {
    // reflection miss is expected on older Hadoop; fall through to alternate class
    get_counter = Class.forName(PACKAGE + ".TaskInputOutputContext")
                       .getMethod("getCounter", String.class, String.class);
}
```

---

## How to Spot

`JVMStabilityInspector.inspectThrowable` is designed for genuinely unexpected exceptions (OOM, assertion errors, disk failures). When a catch block is part of an intentional try/fallback pattern — especially in version-detection or capability-probing code — routing the caught exception through `inspectThrowable` is incorrect. Reviewers should check every `JVMStabilityInspector.inspectThrowable` call to confirm the exception is unexpected and not a normal branch of control flow.

---

## Test

No test added.
