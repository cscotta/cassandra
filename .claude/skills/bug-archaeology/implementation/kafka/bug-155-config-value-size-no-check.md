**Commit**: 42a200bd39 · **Ticket**: KAFKA-18907
**Subsystem**: metadata/src/main/java/org/apache/kafka/controller/ConfigurationControlManager.java
**Category**: Missing null / bounds check

**Root cause**
In KRaft mode, appending configuration values can produce a combined string longer than `Short.MAX_VALUE` bytes, which the metadata log cannot store. Previously no check existed: the oversized value would be written and a generic server-side error was returned to the user with no explanation. The fix adds an explicit length check before applying the `APPEND` operation; if the resulting value exceeds `Short.MAX_VALUE` bytes, a descriptive `INVALID_CONFIG` error is returned immediately.

**Before/After**
```java
// Before: only null-deletion handled, no size check
} else if (configRecord.value() == null) {
    allConfigs.remove(configRecord.name());
} else {
    allConfigs.put(configRecord.name(), configRecord.value());
}

// After: size guard before storing
} else if (configRecord.value() == null) {
    allConfigs.remove(configRecord.name());
} else if (configRecord.value().length() > Short.MAX_VALUE) {
    return DISALLOWED_CONFIG_VALUE_SIZE_ERROR;
} else {
    allConfigs.put(configRecord.name(), configRecord.value());
}
```

**How to spot**
Look for code paths that mutate configuration values (especially append operations) without validating the resulting size before persisting them to storage systems that have hard limits. In KRaft, metadata records encode string lengths as `short`, so any value larger than 32,767 bytes silently breaks the encoding.

**Test**
`ConfigurationControlManagerTest`: creates a string of length `Short.MAX_VALUE - APPEND.id() - 1`, appends it to an existing value, and asserts that the response contains `Errors.INVALID_CONFIG` with the exact human-readable message about the size limit.
