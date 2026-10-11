# Bug 0201: InaccessibleObjectException Accessing java.nio.Bits.TOTAL_CAPACITY on Java 17

**Commit**: 2b23fc58aa · **Ticket**: CASSANDRA-18848
**Subsystem**: service/GCInspector, JVM configuration
**Category**: Missing null / bounds check

---

## Root Cause

`GCInspector` uses reflection to read the `TOTAL_CAPACITY` field from the package-private class `java.nio.Bits` in order to track off-heap direct buffer usage after GC events. Under Java 17's strong module encapsulation, calling `field.setAccessible(true)` on a field in `java.base/java.nio` throws `InaccessibleObjectException` unless the module is explicitly opened. The JVM option `--add-opens java.base/java.nio=ALL-UNNAMED` was missing from the server, client, and build configuration files, so any Cassandra 5.0 node starting under Java 17 would crash or lose the off-heap tracking metric at startup.

### Before / After

```java
// BEFORE – no --add-opens for java.nio in jvm17-server.options / jvm17-clients.options
// field.setAccessible(true) threw InaccessibleObjectException at runtime on Java 17

Field f = bitsClass.getDeclaredField("TOTAL_CAPACITY");
f.setAccessible(true);   // InaccessibleObjectException under Java 17 without --add-opens
```

```java
// AFTER – --add-opens java.base/java.nio=ALL-UNNAMED added to all relevant config files;
// code simplified to only look up the Java 11+ field name

Field f = bitsClass.getDeclaredField("TOTAL_CAPACITY");
f.setAccessible(true);   // succeeds with the new --add-opens flag
```

---

## How to Spot

Any Cassandra code that uses reflection to access fields, methods, or constructors inside a JDK module requires an explicit `--add-opens` entry for the corresponding module/package pair when running under Java 9+. During a Java-version upgrade, audit every `setAccessible(true)` call and cross-check that the enclosing package has a matching `--add-opens` in all relevant JVM options files (server, clients, and build).

---

## Test

No test added.
