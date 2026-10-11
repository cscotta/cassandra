# Bug 1891: Fix NPE if StorageService.getOperationMode() is executed before startup

**Commit**: c198b76c46 · **Ticket**: CASSANDRA-6166
**Subsystem**: src/java/org/apache/cassandra/service
**Category**: Missing null / bounds check

---

## Root Cause

`StorageService.operationMode` was declared without an initializer, leaving it `null` until the startup sequence explicitly set it. Any JMX client or internal code calling `getOperationMode()` before `initServer()` completed would receive a `NullPointerException` when attempting to call `.toString()` on the `null` enum field. The fix adds a new `STARTING` enum constant and assigns it as the field's initial value, so the mode is always non-null from object construction.

### Before / After

```java
// BEFORE
private static enum Mode { NORMAL, CLIENT, JOINING, LEAVING, DECOMMISSIONED, MOVING, DRAINING, DRAINED, RELOCATING }
private Mode operationMode;
```

```java
// AFTER
private static enum Mode { STARTING, NORMAL, CLIENT, JOINING, LEAVING, DECOMMISSIONED, MOVING, DRAINING, DRAINED, RELOCATING }
private Mode operationMode = Mode.STARTING;
```

---

## How to Spot

Any mutable field of an enum or object type that lacks a field initializer is a latent NPE if code can observe the object before assignment. Review all public/JMX-exposed getters to confirm their backing fields are initialized at declaration or in the constructor.

---

## Test

`StorageServiceServerTest.testGetOperationMode()` — calls `getOperationMode()` before server startup and asserts the result equals `"STARTING"`.
