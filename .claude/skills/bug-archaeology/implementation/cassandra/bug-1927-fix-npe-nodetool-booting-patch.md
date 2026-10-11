# Bug 1927: NPE in nodetool status checks when thriftServer or nativeServer is null during boot

**Commit**: 08c07f5f32 · **Ticket**: CASSANDRA-5968
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Missing null / bounds check

---

## Root Cause

`isRPCServerRunning()` and `isNativeTransportRunning()` in `StorageService` guarded against a null `daemon` reference but did not account for the case where `daemon` is non-null yet `daemon.thriftServer` or `daemon.nativeServer` is still `null` during node boot. When nodetool queried server status before the daemon had fully initialized its server fields, the subsequent `.isRunning()` call on the null server reference threw a `NullPointerException`.

### Before / After

```java
// BEFORE
public boolean isRPCServerRunning() {
    if (daemon == null) {
        return false;
    }
    return daemon.thriftServer.isRunning(); // NPE if thriftServer is null
}

public boolean isNativeTransportRunning() {
    if (daemon == null) {
        return false;
    }
    return daemon.nativeServer.isRunning(); // NPE if nativeServer is null
}
```

```java
// AFTER
public boolean isRPCServerRunning() {
    if ((daemon == null) || (daemon.thriftServer == null)) {
        return false;
    }
    return daemon.thriftServer.isRunning();
}

public boolean isNativeTransportRunning() {
    if ((daemon == null) || (daemon.nativeServer == null)) {
        return false;
    }
    return daemon.nativeServer.isRunning();
}
```

---

## How to Spot

Multi-level null guards must be complete: guarding the top-level object is not sufficient when the code immediately dereferences a field of that object. Review every dereference chain `a.b.method()` to ensure both `a` and `a.b` are null-checked, especially for fields that are initialized lazily or sequentially during startup.

---

## Test

No test added.
