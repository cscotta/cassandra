# Bug 0548: MBeanWrapper Uses java.util.function.Consumer, Breaking JDK 7 Compatibility

**Commit**: a9a4f171be · **Ticket**: CASSANDRA-15050
**Subsystem**: `src/java/org/apache/cassandra/utils/MBeanWrapper.java`
**Category**: Wrong constant / default value

---

## Root Cause

The cassandra-2.2 branch still targeted JDK 7 as a runtime, but `MBeanWrapper` was written with an import of `java.util.function.Consumer` — a class introduced in Java 8. The build target was also erroneously set to `source/target=1.7` while using Java 8 APIs, which allowed the code to compile (because the JDK 8 compiler was used) but fail at runtime on JDK 7. The fix both raised the compile target to 1.8 (aligning intent with reality) and replaced the JDK import with a locally defined `Consumer<T>` interface inside `MBeanWrapper` as a compatibility shim for any JDK 7 deployment.

### Before / After

```java
// BEFORE (MBeanWrapper.java)
import java.util.function.Consumer;
// ... uses Consumer<T> from java.util.function (Java 8+)
```

```java
// AFTER (MBeanWrapper.java)
// No import; local interface added at bottom of file:
interface Consumer<T> {
    void accept(T e);
}
```

---

## How to Spot

When a codebase claims to support an older Java version, verify that `build.xml` or `pom.xml` source/target levels match and that no import in production code refers to a type introduced in a later JDK. CI jobs that compile with `javac -source 1.7 -target 1.7` but use a JDK 8+ compiler will still accept JDK 8 API imports, producing class files that crash at runtime on JDK 7.

---

## Test

No test added; the fix resolves a compilation/runtime incompatibility.
