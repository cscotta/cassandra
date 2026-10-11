# Bug 0572: ProtocolVersion.decode Pulls Config from DatabaseDescriptor Directly

**Commit**: bd0cef9a36 · **Ticket**: CASSANDRA-14800
**Subsystem**: `src/java/org/apache/cassandra/transport/ProtocolVersion.java`
**Category**: Wrong constant / default value

---

## Root Cause

`ProtocolVersion.decode` called `DatabaseDescriptor.getNativeTransportAllowOlderProtocols()` internally to decide whether to reject older protocol versions. This created a hidden dependency on the global server configuration inside an enum utility method, making it impossible to call `decode` in contexts where `DatabaseDescriptor` is not initialized (e.g., standalone tools, tests, or the FQL replay tool). The fix removes the `DatabaseDescriptor` import from `ProtocolVersion` and makes the flag an explicit parameter, letting callers supply the appropriate value — the server supplies the live config value while tools supply `true` to always allow reading stored protocol versions.

### Before / After

```java
// BEFORE
public static ProtocolVersion decode(int versionNum)
{
    ...
    if (!DatabaseDescriptor.getNativeTransportAllowOlderProtocols() && ret.isSmallerThan(CURRENT))
        throw new ProtocolException(...);
    return ret;
}
```

```java
// AFTER
public static ProtocolVersion decode(int versionNum, boolean allowOlderProtocols)
{
    ...
    if (!allowOlderProtocols && ret.isSmallerThan(CURRENT))
        throw new ProtocolException(...);
    return ret;
}
```

---

## How to Spot

When a low-level utility class (enum, codec, parser) calls `DatabaseDescriptor` or any other singleton that requires full server initialization, it becomes unusable in tool or test contexts that skip daemon startup. Review utility methods that implicitly read global configuration; configuration-dependent behavior should be passed as a parameter so the method remains context-neutral and testable in isolation.

---

## Test

Existing `ProtocolVersionTest.testDecode` was updated to pass the flag explicitly; the FQL tool tests were updated similarly to pass `true`, confirming that the method remains correct with the extracted parameter.
