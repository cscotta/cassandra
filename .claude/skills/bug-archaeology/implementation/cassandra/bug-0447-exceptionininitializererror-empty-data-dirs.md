# Bug 0447: ExceptionInInitializerError When data_file_directories Is Empty Array

**Commit**: ed80055523 · **Ticket**: CASSANDRA-16008
**Subsystem**: src/java/org/apache/cassandra/config/DatabaseDescriptor.java
**Category**: Missing null / bounds check

---

## Root Cause

`DatabaseDescriptor.applyConfig()` checked `if (conf.data_file_directories == null)` to detect unconfigured data directories and apply a default. However, YAML deserialization of an explicitly-empty `data_file_directories: []` produces a non-null but zero-length array. The null check passed, the zero-length array was accepted as valid, and downstream code that iterated or indexed into the array (before a directory was available) threw an `ExceptionInInitializerError` at class-load time, making the root cause invisible and the error message unhelpful.

### Before / After

```java
// BEFORE — zero-length array passes the null check, causing ExceptionInInitializerError later
if (conf.data_file_directories == null)
{
    // apply default ...
}
```

```java
// AFTER — treat empty array the same as null
if (conf.data_file_directories == null || conf.data_file_directories.length == 0)
{
    // apply default ...
}
```

---

## How to Spot

Wherever configuration arrays are checked for "not set", both `null` and `length == 0` must be treated as absent. YAML allows an explicit empty list, which deserializes to a non-null zero-length Java array; any null-only guard will miss this case. Review all configuration validation code that reads array-typed config fields for this pattern.

---

## Test

No test added.
