# Bug 0624: JVM Version Parser Crashes on Non-Standard Version Strings

**Commit**: b800f3c768 · **Ticket**: CASSANDRA-14112
**Subsystem**: `src/java/org/apache/cassandra/service/StartupChecks.java`, `org/apache/cassandra/utils/JavaUtils.java`
**Category**: Logic error in condition

---

## Root Cause

The startup OOM-handling check parsed the JVM version string with two private methods, `getJavaVersion()` and `getUpdate()`, that were inlined directly into `StartupChecks`. The `getJavaVersion()` method called `jreVersion.indexOf('.')` without handling the case where no dot exists (e.g., `"9"` or `"9-ea"` for early-access Java 9 builds), causing an `IndexOutOfBoundsException` or a `NumberFormatException` from an empty substring. Likewise, `getUpdate()` called `Integer.parseInt` on the substring after `_`, but pre-9 EA versions such as `"1.7.0-ea"` contain no underscore, and the `substring(updateSeparatorIndex + 1)` then parsed the full version string, again throwing. The check was therefore unreliable for any non-GA JVM string.

### Before / After

```java
// BEFORE – crashes on "9", "9-ea", "1.7.0-ea"
private int getJavaVersion() {
    String jreVersion = System.getProperty("java.version");
    String version = jreVersion.startsWith("1.") ? jreVersion.substring(2, 3)
                                                 : jreVersion.substring(0, jreVersion.indexOf('.'));
    return Integer.parseInt(version); // indexOf('.') == -1 for "9"
}
private int getUpdate() {
    String jreVersion = System.getProperty("java.version");
    int updateSeparatorIndex = jreVersion.indexOf('_');
    return Integer.parseInt(jreVersion.substring(updateSeparatorIndex + 1)); // -1+1 == 0 for "1.7.0-ea"
}
```

```java
// AFTER – handles EA, missing update, and Java >=9
public static boolean supportExitOnOutOfMemory(String jreVersion) {
    try {
        int version = parseJavaVersion(jreVersion);      // handles missing dot
        if (version > 8) return true;
        int update = parseUpdateForPre9Versions(jreVersion); // handles missing _
        return (version == 7 && update >= 101) || (version == 8 && update >= 92);
    } catch (Exception e) {
        logger.error(...);
        return true; // safe default
    }
}
```

---

## How to Spot

Any time a version string is parsed by manually searching for delimiter characters (`indexOf`, `substring`) without checking for -1 or handling absent segments, the code will fail on non-conforming inputs such as early-access releases, release candidates, or major-version-only strings. Reviewers should look for chains of `indexOf` + `substring` without a guard on the separator index, and verify all known version-string formats (GA, EA, major-only) against the parser.

---

## Test

`JavaUtilsTest.testSupportExitOnOutOfMemory` exercises EA strings (`1.7.0-ea`, `9-ea`), bare major-version strings (`9`), and update variants (`1.7.0_95`, `1.8.0_92`) to assert the correct boolean is returned without exceptions.
