# Bug 1779: Fix NPE when stopping nonexistent native server

**Commit**: 723fcd1936 · **Ticket**: n/a
**Subsystem**: service (StorageService)
**Category**: Missing null / bounds check

---

## Root Cause

`StorageService.stopNativeTransport()` called `daemon.nativeServer.stop()` unconditionally. When the native (CQL binary) transport was never started — for example, when `start_native_transport: false` in the configuration — `daemon.nativeServer` is `null`, causing a `NullPointerException`. The fix adds a null guard before invoking `stop()`.

### Before / After

```java
// BEFORE
daemon.nativeServer.stop();
```

```java
// AFTER
if (daemon.nativeServer != null)
    daemon.nativeServer.stop();
```

---

## How to Spot

Any optional subsystem (one that may not be initialized depending on configuration) must have its reference null-checked before calling methods on it. When a field is conditionally assigned, mark it clearly as nullable and add guards at every call site.

---

## Test

No test added.
