# Bug 1642: Fix NPE When configURL Is Null

**Commit**: 3fb2dcdf06 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/locator (SnitchProperties)
**Category**: Missing null / bounds check

---

## Root Cause

`SnitchProperties` reads an optional system property `cassandra-rackdc.properties` and falls back to loading the file from the classloader when the property is absent. The original code called `new URL(configURL)` unconditionally before checking whether `configURL` was null. Since `new URL(null)` throws a `MalformedURLException` (or `NullPointerException` depending on the JVM), the fallback path was never reachable — any deployment that did not set the system property would fail immediately on startup.

### Before / After

```java
// BEFORE
URL url = new URL(configURL);          // throws when configURL is null
if (configURL == null)
    url = SnitchProperties.class.getClassLoader().getResource(RACKDC_PROPERTY_FILENAME);
```

```java
// AFTER
URL url;
if (configURL == null)
    url = SnitchProperties.class.getClassLoader().getResource(RACKDC_PROPERTY_FILENAME);
else
    url = new URL(configURL);
```

---

## How to Spot

Whenever a null-check on a variable appears after the variable has already been dereferenced or passed to a constructor, the check is dead code — the exception will have already been thrown. Always perform null checks before use.

---

## Test

No test added.
