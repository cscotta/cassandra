# Bug 0174: ExceptionInInitializerError on Startup When Upgrading from 5.0-beta1

**Commit**: 7f354571f3 · **Ticket**: CASSANDRA-19263
**Subsystem**: net/MessagingService, service/CassandraDaemon
**Category**: Missing null / bounds check

---

## Root Cause

After the refactor in CASSANDRA-19126, `MessagingService.Version.CURRENT` was replaced with a `static` initializer block that calls `DatabaseDescriptor.getStorageCompatibilityMode()`. When upgrading from 5.0-beta1 to trunk, the `system_cluster_metadata` table could be absent or in an unexpected state, causing `CassandraDaemon.initializeClusterMetadataService()` to throw. That exception was caught and rethrown as `AssertionError("Can't initialize cluster metadata service")` without chaining the cause, making the root cause invisible in logs. Separately, the `static {}` block in `MessagingService.Version` lacked a logger, so any exception thrown from `DatabaseDescriptor.getStorageCompatibilityMode()` during class loading surfaced as a bare `ExceptionInInitializerError` with no diagnostic information. The fix adds a `Logger` to the `Version` inner class and chains the original exception cause in the `AssertionError`.

### Before / After

```java
// BEFORE – cause swallowed
throw new AssertionError("Can't initialize cluster metadata service");
```

```java
// AFTER – cause preserved
throw new AssertionError("Can't initialize cluster metadata service", e);
```

---

## How to Spot

`AssertionError` and similar wrapping exceptions should always chain the original throwable so that the root cause is visible in logs and stack traces. Review all `throw new AssertionError("message")` or `throw new RuntimeException("message")` in catch blocks — if the caught variable is not passed as the second argument, diagnostic information is silently discarded. Additionally, `static {}` class initializers that call into external subsystems should declare a logger so that failures during class loading produce actionable output.

---

## Test

No new test added; the fix was confirmed by successfully completing a rolling upgrade from 5.0-beta1 to trunk in integration testing.
