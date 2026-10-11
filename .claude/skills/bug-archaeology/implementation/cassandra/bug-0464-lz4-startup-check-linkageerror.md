# Bug 0464: LZ4 Startup Check Misses LinkageError

**Commit**: 3cca9ab22f · **Ticket**: CASSANDRA-15929
**Subsystem**: `src/java/org/apache/cassandra/service/StartupChecks.java`
**Category**: Logic error in condition

---

## Root Cause

The LZ4 native library startup check only caught `AssertionError` when attempting to load the native instance, but class-loading failures manifest as `LinkageError` (a `NoClassDefFoundError` or `UnsatisfiedLinkError`), which is not a subclass of `AssertionError`. When the native library was unavailable or could not be linked, a `LinkageError` escaped the catch block entirely, crashing startup rather than gracefully falling back with a warning.

### Before / After

```java
// BEFORE
try {
    LZ4Factory.nativeInstance();
}
catch (AssertionError e) {
    logger.warn("lz4-java was unable to load native libraries ...");
}
```

```java
// AFTER
try {
    LZ4Factory.nativeInstance();
}
catch (AssertionError | LinkageError e) {
    logger.warn("lz4-java was unable to load native libraries ...");
}
```

---

## How to Spot

When a `try` block loads native libraries or performs class initialization, the catch clause should include `LinkageError` (or its subtypes `NoClassDefFoundError`, `UnsatisfiedLinkError`) alongside application-level exceptions. Catching only `AssertionError` works for internal library self-checks but misses JVM-level class-loading failures that surface as `Error` subtypes outside the `AssertionError` hierarchy.

---

## Test

No test added.
