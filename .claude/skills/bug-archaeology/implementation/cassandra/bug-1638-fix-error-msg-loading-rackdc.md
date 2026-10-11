# Bug 1638: Fix Error Message Loading Rackdc Properties When System Property Is Used

**Commit**: 69e65be8d7 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/locator
**Category**: Logic error in condition

---

## Root Cause

`SnitchProperties` supports loading `cassandra-rackdc.properties` either from the default classpath location or from a path supplied via a system property (`configURL`). When the file could not be read, the warning message always logged the constant `RACKDC_PROPERTY_FILENAME` regardless of which source was actually being used. If a system property path was configured, the error message therefore named the wrong file, making it harder to diagnose configuration problems.

### Before / After

```java
// BEFORE
logger.warn("Unable to read " + RACKDC_PROPERTY_FILENAME);
```

```java
// AFTER
logger.warn("Unable to read {}", ((configURL != null) ? configURL : RACKDC_PROPERTY_FILENAME));
```

---

## How to Spot

When a method supports multiple sources for a resource (file path, URL, classpath default), error messages should always reference the actual source in use, not a hardcoded constant. Review error/warning paths in any code that has a "default vs. override" branching pattern.

---

## Test

No test added.
