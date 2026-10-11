**Commit**: 1660289da2 · **Ticket**: KAFKA-19792
**Subsystem**: build.gradle (connect:runtime project / genConnectOpenAPIDocs task)
**Category**: Wrong constant / default value

**Root cause**: Swagger Gradle Plugin 2.2.27+ migrated property values to Gradle's lazy `Property<T>` API. When the `genConnectOpenAPIDocs` task manually instantiates a `ResolveTask` and queries those properties, Gradle throws `Cannot query the value of property 'xyz' because it has no value available` because the manually-created instance bypasses the plugin's convention-setting code. The fix explicitly initialises each required property (`encoding`, `alwaysResolveAppPath`, `skipResolveAppPath`, `readAllResources`) to the same defaults the plugin would have set.

**Before/After**:
```groovy
// BEFORE – properties left unset; queries throw at task execution time
prettyPrint = 'TRUE'  // string instead of boolean
sortOutput = 'TRUE'

// AFTER – all required lazy properties explicitly initialised
prettyPrint = true
sortOutput = true
encoding = 'UTF-8'
alwaysResolveAppPath = false
skipResolveAppPath = false
readAllResources = true
```

**How to spot**: When a Gradle task is instantiated manually (rather than via `tasks.register` with plugin conventions), any `Property<T>` that the plugin normally populates will be empty. Bump any plugin that uses lazy configuration and watch for `Cannot query the value of property` errors during task execution.

**Test**: No test added. The fix is a build script correction verified by successfully running `./gradlew genConnectOpenAPIDocs` after the Swagger version upgrade to 2.2.39.
