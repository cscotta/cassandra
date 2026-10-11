# Bug 0796: UDF Sandbox Can Reload logback.xml via ReconfigureOnChangeFilter

**Commit**: 8f15eb1b71 · **Ticket**: CASSANDRA-12535
**Subsystem**: UDF sandbox / Security / Logging
**Category**: State not cleaned up

---

## Root Cause
Logback's `ReconfigureOnChangeFilter` periodically rescans and reloads `logback.xml`. When a User Defined Function runs in the `ThreadAwareSecurityManager` sandbox, logback's background turbo-filter fires on each log call and may detect a file change, triggering `ReconfigureOnChangeFilter.changeDetected()` — which then loads and re-parses the config file. This occurs on the sandboxed UDF thread, bypassing any security restrictions that should have prevented file-system access. The fix wraps the existing `ReconfigureOnChangeFilter` with `SMAwareReconfigureOnChangeFilter`, which overrides `changeDetected()` to return `false` unconditionally when the calling thread is a secured (sandboxed) thread, preventing any logback reconfiguration from within UDF execution.

### Before / After
```java
// BEFORE — ReconfigureOnChangeFilter installed as-is; fires on UDF threads
System.setSecurityManager(new ThreadAwareSecurityManager());
installed = true;

// AFTER — wrap with sandbox-aware subclass that suppresses change detection
for (TurboFilter tf : ctx.getTurboFilterList()) {
    if (tf instanceof ReconfigureOnChangeFilter) {
        turboFilterList.set(i, new SMAwareReconfigureOnChangeFilter((ReconfigureOnChangeFilter) tf));
        break;
    }
}

// SMAwareReconfigureOnChangeFilter:
protected boolean changeDetected(long now) {
    if (isSecuredThread())
        return false;          // no reload from UDF sandbox
    return super.changeDetected(now);
}
```

---

## How to Spot
Security sandbox escapes often hide in framework callbacks that are invoked transparently on any thread. Look for long-running or frequently invoked code paths that use frameworks (logging, metrics, JMX) with background-activity hooks and verify that those hooks cannot execute privileged operations when called from a sandboxed thread. The pattern to search for is: framework filter/listener + `isSecuredThread()` check missing.

---

## Test
`AggregationTest` was updated to run the UDF for a time period (150 ms) rather than a fixed iteration count, increasing the chance that the logback refresh period elapses and the reconfiguration would have been triggered without the fix.
