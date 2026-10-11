# Bug 1960: Trigger directory resolved from wrong base path

**Commit**: 4f2fdc2e73 · **Ticket**: CASSANDRA-5826
**Subsystem**: src/java/org/apache/cassandra/triggers/TriggerExecutor.java, src/java/org/apache/cassandra/utils/FBUtilities.java
**Category**: Wrong constant / default value

---

## Root Cause

`TriggerExecutor` located the trigger directory by appending `"triggers"` to `cassandraHomeDir()`, which was derived by walking two levels up from the JAR's code source location on the filesystem. This path is not the conf directory and is unreliable in packaged deployments (e.g., Debian), so triggers were never found. The fix introduces a dedicated `cassandraTriggerDir()` method that resolves the directory from the classloader resource `"triggers"` (which maps to `conf/triggers/`) or from the overridable system property `cassandra.triggers_dir`.

### Before / After

```java
// BEFORE
private final File triggerDirectory = new File(FBUtilities.cassandraHomeDir(), "triggers");

// cassandraHomeDir() derived from JAR location:
public static File cassandraHomeDir() {
    String libDir = new File(FBUtilities.class.getProtectionDomain()
        .getCodeSource().getLocation().getPath()).getParent();
    return new File(new File(libDir).getParent());
}
```

```java
// AFTER
private final File triggerDirectory = FBUtilities.cassandraTriggerDir();

public static File cassandraTriggerDir() {
    File triggerDir;
    if (System.getProperty("cassandra.triggers_dir") != null)
        triggerDir = new File(System.getProperty("cassandra.triggers_dir"));
    else
        triggerDir = new File(FBUtilities.class.getClassLoader()
            .getResource(DEFAULT_TRIGGER_DIR).getFile());
    if (!triggerDir.exists())
        throw new RuntimeException("Trigger Directory doesnt exist, please create inside conf.");
    return triggerDir;
}
```

---

## How to Spot

Whenever a component resolves a configuration directory by walking up the filesystem tree from the JVM's code-source location, it will break in any non-standard packaging layout. Configuration paths should be derived from the classloader resource hierarchy or from an explicit system property, not from the location of the JAR file.

---

## Test

No test added. The `conf/triggers/README.txt` and packaging files (`debian/cassandra.install`) were updated to put the directory in the right place.
