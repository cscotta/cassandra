**Commit**: 00b22b001f · **Ticket**: KAFKA-18211
**Subsystem**: connect/runtime
**Category**: Incorrect filtering / wrong query result

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
                .addClassLoader(source.loader())
        SUBCLASS_OF_CLASSPATH("subclass-of-classpath");

// After
import java.util.ArrayList;
import java.util.List;
                .overrideClassLoaders(classLoaderOrder(source))
    private ClassLoader[] classLoaderOrder(PluginSource source) {
        // Classgraph will first scan all the class URLs from the provided classloader chain and use said chain during classloading.
        // We compute and provide the classloader chain starting from the isolated PluginClassLoader to ensure that it adheres
        // to the child first delegation model used in connect. In addition, classgraph can fail to find URLs from the
        // application classloader as it uses an illegal reflections access. Providing the entire chain of classloaders
        // which included the application classloader forces classpath URLs to be scanned separately.
        List<ClassLoader> classLoaderOrder = new ArrayList<>();
        ClassLoader cl = source.loader();
        while (cl != null) {
```

**How to spot**
When reviewing connect/runtime code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18211: override class loaders for class graph scanning in connect. (#18403).

**Test**
Test modified in PluginScannerTest.java covering the corrected behaviour.
