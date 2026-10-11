# Bug 0243: Trigger JAR Copy Fails When Destination Already Exists

**Commit**: 9860c1e9d9 · **Ticket**: CASSANDRA-18264
**Subsystem**: triggers/CustomClassLoader
**Category**: Non-atomic file write

---

## Root Cause

`CustomClassLoader.addJarToClasspath()` used `Files.copy(src, dst)` without `StandardCopyOption.REPLACE_EXISTING`. The JDK `Files.copy` contract throws `FileAlreadyExistsException` if the destination file already exists. On the first deployment of a trigger the copy succeeded, but on any subsequent deployment (e.g., after a Cassandra restart, or after redeploying the same trigger JAR) the temporary destination already existed and the copy threw an exception, preventing the trigger from being (re)loaded. Adding `StandardCopyOption.REPLACE_EXISTING` allows the copy to overwrite the existing file.

### Before / After

```java
// BEFORE
copy(inputJar.toPath(), out.toPath());
```

```java
// AFTER
copy(inputJar.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
```

---

## How to Spot

Any use of `Files.copy(src, dst)` without `REPLACE_EXISTING` in code that may run more than once for the same destination path will fail on the second run. Review every `Files.copy` call and ask whether the destination could already exist; if yes, an explicit copy-option (`REPLACE_EXISTING` or `ATOMIC_MOVE`) is required. This is especially common in class-loader and plugin-loading paths where JARs are staged to a work directory on each server start.

---

## Test

No automated test added for the copy-option fix itself; the correctness is validated by manual trigger deployment and the existing trigger integration tests.
