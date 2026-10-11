**Commit**: 9f822fcce1 · **Ticket**: KAFKA-19786
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
In Scala/Java joint compilation, builds that run with
`keepAliveMode=SESSION` can fail in the `:core:compileScala` task. This
happens when `javac` is given a non-existent Java output directory on
its classpath. ```
Unexpected javac output: warning: [path] bad path element
".../core/build/classes/java/main": no such file or directory
error: warnings found and -Werror specified
```

This issue occurs after a clean build because
`core/build/classes/java/main` does not exist, as Java classes are
written to `classes/scala/main` during joint compilation. With `-Werror`
enabled, the resulting `[path]` warning is treated as an error and stops
the build.

**Before/After**
```java
// Before
// (no removals)

// After
      if (userKeepAliveMode == KeepAliveMode.SESSION){
        // Suppress warning triggered under keepAliveMode=SESSION during joint compilation
        // Unexpected javac output: warning: [path] bad path element ".../core/build/classes/java/main": no such file or directory
        options.compilerArgs << "-Xlint:-path"
      }
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: prevent joint compilation error in core under session keepalivemode (#20695).

**Test**
No test added.
