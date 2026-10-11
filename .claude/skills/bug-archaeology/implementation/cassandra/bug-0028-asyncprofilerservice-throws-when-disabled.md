# Bug 0028: AsyncProfilerService Throws Exception When Profiler Is Disabled

**Commit**: d1b4ddc32a · **Ticket**: CASSANDRA-21135
**Subsystem**: service/AsyncProfilerService
**Category**: Logic error in condition

---

## Root Cause

`AsyncProfilerService.status()` delegated unconditionally to the native-library call path without first checking whether the async profiler was enabled. When `cassandra.async_profiler.enabled` was set to `false`, the call threw an exception rather than returning a descriptive message. JMX clients and the `JMXFeatureTest` expected `status()` to always return a string, so the exception caused test and operational failures. The fix adds an early-return guard that produces a human-readable message when the profiler is disabled.

### Before / After

```java
// BEFORE
public String status()
{
    return run(new ThrowingFunction<>() { ... });  // throws if profiler disabled
}
```

```java
// AFTER
public String status()
{
    if (!ASYNC_PROFILER_ENABLED.getBoolean())
        return "Async Profiler is not enabled. Enable it by setting "
               + ASYNC_PROFILER_ENABLED.getKey() + " property to true.";
    return run(new ThrowingFunction<>() { ... });
}
```

---

## How to Spot

Any JMX MBean method that is expected to always return a value (rather than throw) must guard against disabled or uninitialized subsystems before delegating to the implementation. Review each `@Override` of a management-interface method and confirm it handles the "subsystem not available" case gracefully.

---

## Test

`AsyncProfilerServiceTest.testProfilerDisabledReturnsMessage` verifies that `status()` returns a string containing the expected message rather than throwing when the profiler is disabled.
