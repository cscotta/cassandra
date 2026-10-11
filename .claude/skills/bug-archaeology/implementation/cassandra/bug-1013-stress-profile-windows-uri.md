# Bug 1013: Stress profile URI broken on Windows

**Commit**: b876424228 · **Ticket**: CASSANDRA-10808
**Subsystem**: cassandra-stress / settings
**Category**: Logic error in condition

---

## Root Cause
`SettingsCommandUser` constructs a URI for the YAML stress profile.  When the path points to a local file it manually prepended `"file:///"` to the absolute path string: `"file:///" + yamlFile.getAbsolutePath()`.  On Windows, `getAbsolutePath()` returns a path starting with a drive letter and backslash (e.g. `C:\Users\...`), producing a malformed URI like `file:///C:\Users\...` where backslashes are not valid URI separators.  The fix replaces the manual string concatenation with `yamlFile.toURI()`, which the JDK implements correctly for all platforms, handling drive letters and path-separator conversion automatically.

### Before / After
```java
// BEFORE – manual string concat breaks on Windows drive letters/backslashes
if (yamlFile.exists())
{
    yamlPath = "file:///" + yamlFile.getAbsolutePath();
}
profile = StressProfile.load(URI.create(yamlPath));

// AFTER – let JDK handle platform differences
profile = StressProfile.load(yamlFile.exists() ? yamlFile.toURI() : URI.create(yamlPath));
```

---

## How to Spot
Wherever code converts a filesystem path to a URI by string concatenation (`"file:///" + path.toString()`), it will be broken on Windows.  Always prefer `File.toURI()` or `Path.toUri()` which handle drive letters and OS-specific separators correctly.  This pattern is especially common in tools that accept a CLI path argument and need to pass it as a URI to a library.

---

## Test
No test added; this is a platform-specific fix validated by running stress on Windows.
