**Commit**: e3bb2b8d01 · **Ticket**: KAFKA-20168
**Subsystem**: gradle/dependencies.gradle
**Category**: Wrong constant / default value

**Root cause**: Jetty 12.0.30+ started using the SLF4J 2.x fluent API (`Logger.atDebug()`) in production code. Kafka's runtime classpath only includes SLF4J 1.7.x, which does not have the fluent API, causing `NoSuchMethodError` at runtime whenever Jetty's production code paths were exercised. A previous commit had upgraded Jetty to 12.0.32 for a CVE fix (CVE-2025-5115), but that version introduced the incompatibility. The fix downgrades to 12.0.25, which contains the same CVE fix while only using the SLF4J 2.x fluent API in Jetty's test code (not shipped), avoiding the runtime error.

**Before/After**:
```groovy
// BEFORE – Jetty 12.0.32 uses SLF4J 2.x fluent API in production code
jetty: "12.0.32"

// AFTER – Jetty 12.0.25 has CVE fix but no SLF4J 2.x production usage
jetty: "12.0.25"
```

**How to spot**: When upgrading a dependency that has logging, check whether the new version requires a logging API version newer than what the project provides. SLF4J 1.x → 2.x is a binary-incompatible change; any library that calls `Logger.atDebug()` or similar fluent methods will fail at runtime on a 1.x classpath.

**Test**: No new test added. The fix is a version downgrade verified by confirming the Connect REST API starts without `NoSuchMethodError`.
