# Bug 1570: Fix compilation with Java 6 (JAVA-7147 regression)

**Commit**: 135513cf9b · **Ticket**: n/a
**Subsystem**: utils / locator
**Category**: Wrong constant / default value

---

## Root Cause

A prior change (JAVA-7147) introduced references to `java.nio.charset.StandardCharsets.UTF_8` and a local `Charset UTF_8` field in `ByteBufferUtil`, both of which require Java 7 or later. Cassandra 2.x targeted Java 6 at the time, so these references broke compilation under JDK 6.

The fix centralizes a single `Charset UTF_8 = Charset.forName("UTF-8")` constant in `FBUtilities` (which is Java-6-compatible) and replaces all uses of `StandardCharsets.UTF_8` and the local `ByteBufferUtil` field with `FBUtilities.UTF_8`.

### Before / After

```java
// BEFORE — uses Java 7 API in ByteBufferUtil.java
import java.nio.charset.StandardCharsets;
// ...
private static final Charset UTF_8 = Charset.forName("UTF-8"); // duplicate field
// ...
return new String(b, StandardCharsets.UTF_8); // Java 7+ only
```

```java
// AFTER — uses shared Java-6-compatible constant from FBUtilities
import java.nio.charset.Charset;
// In FBUtilities.java:
public static final Charset UTF_8 = Charset.forName("UTF-8");

// Callers:
return new String(b, FBUtilities.UTF_8);
```

---

## How to Spot

When a project supports a minimum JDK version, review any new imports from packages introduced in later JDKs (e.g., `java.nio.charset.StandardCharsets` requires Java 7). A shared utility constant for common charsets avoids scattering language-version-sensitive literals throughout the codebase.

---

## Test

No test added in this commit.
