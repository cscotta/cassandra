# Bug 0037: StorageService JMX MBean Not Available During Bootstrap

**Commit**: 07163cfb51 · **Ticket**: CASSANDRA-19902
**Subsystem**: service
**Category**: State not cleaned up

---

## Root Cause

`StorageService.registerMBeans()` was only called inside `completeInitialization()`, which ran after the node finished bootstrapping. During the bootstrap phase the JMX MBean was therefore not yet registered, making it impossible for operators to query node status (e.g., `getOperationMode()`) via JMX tools while bootstrap was in progress. The fix moves `registerMBeans()` to be called unconditionally at the start of `initServer`, well before the bootstrap code runs, and removes the conditional guard in `completeInitialization`.

### Before / After

```java
// BEFORE: registerMBeans only after bootstrap
public void completeInitialization() {
    if (!initialized)
        registerMBeans();   // too late — bootstrap already happened
    initialized = true;
}
```

```java
// AFTER: register early in initServer, before bootstrap
Runtime.getRuntime().addShutdownHook(drainOnShutdown);
registerMBeans();   // moved here
// ... rest of initServer including bootstrap ...

public void completeInitialization() {
    initialized = true;   // registerMBeans() removed from here
}
```

---

## How to Spot

Whenever an operational interface (JMX, HTTP health endpoint, metrics) is only registered at the end of an initialization sequence, operators lose visibility into long-running intermediate phases such as bootstrap, repair, or schema migration. Management interfaces should be registered as early as possible in the startup sequence, before any blocking or time-consuming steps begin.

---

## Test

`BootstrapTest.testStorageServiceMBeanIsPublishedOnJMXDuringBootstrap` pauses bootstrap via a `ByteBuddy` interceptor and then connects via JMX to assert `getOperationMode()` returns `JOINING`.
