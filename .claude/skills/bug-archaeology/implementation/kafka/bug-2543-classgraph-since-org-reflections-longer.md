**Commit**: e24354a21d · **Ticket**: KAFKA-15203
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Liam Miller-Cushon <cushon@google.com>, Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
    implementation libs.reflections
      <allow pkg="org.reflections"/>
      <allow pkg="org.reflections.util"/>
      <allow pkg="org.reflections.vfs" />
import org.reflections.util.ClasspathHelper;
        parentUrls.addAll(ClasspathHelper.forJavaClassPath());
        parentUrls.addAll(ClasspathHelper.forClassLoader(classLoader));
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import java.util.Set;

// After
    implementation libs.classgraph
      <allow pkg="io.github.classgraph"/>
import java.io.File;
import java.net.URLClassLoader;
import java.util.Collection;
        parentUrls.addAll(forJavaClassPath());
        parentUrls.addAll(forClassLoader(classLoader));
    private static Collection<URL> forJavaClassPath() {
        Collection<URL> urls = new ArrayList<>();
        String javaClassPath = System.getProperty("java.class.path");
        if (javaClassPath != null) {
            for (String path : javaClassPath.split(File.pathSeparator)) {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15203: use classgraph since org.reflections is no longer under maintenance (#16604).

**Test**
No test added.
