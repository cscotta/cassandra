**Commit**: af0054b502 · **Ticket**: KAFKA-18182
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
    PluginClassLoader pluginClassLoader(String name) {
        ClassLoader pluginLoader = inner.get(inner.lastKey());
               ? (PluginClassLoader) pluginLoader
               : null;
    ClassLoader connectorLoader(String connectorClassOrAlias) {
        String fullName = aliases.getOrDefault(connectorClassOrAlias, connectorClassOrAlias);
        ClassLoader classLoader = pluginClassLoader(fullName);
        if (classLoader == null) classLoader = this;
            "Getting plugin class loader: '{}' for connector: {}",
            classLoader,
            connectorClassOrAlias

// After
    implementation libs.slf4jReload4j
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
     *
    PluginClassLoader pluginClassLoader(String name, VersionRange range) {



```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18182: kip-891 add versionrange to plugins and delegatingclassloader apis (#16984).

**Test**
Test modified in SynchronizationTest.java covering the corrected behaviour.
