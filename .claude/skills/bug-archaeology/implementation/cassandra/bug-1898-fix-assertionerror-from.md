# Bug 1898: AssertionError from Missing BOOTSTRAP_TOKEN Case in Timeout Lookup

**Commit**: 92b3622dc2 · **Ticket**: n/a (references #6132)
**Subsystem**: src/java/org/apache/cassandra/net, src/java/org/apache/cassandra/dht
**Category**: Missing null / bounds check

---

## Root Cause

`DatabaseDescriptor.getTimeout(MessagingService.Verb)` lacked a case for `Verb.BOOTSTRAP_TOKEN`, causing a fall-through to the `default` branch which would throw an `AssertionError`. The fix adds the missing case and also makes `BOOTSTRAP_TIMEOUT` public so `DatabaseDescriptor` can reference it. Additionally, `MessagingService.sendRR` was refactored to accept an optional `ConsistencyLevel` and route to the appropriate `addCallback` overload, with `null` used for non-CL paths to avoid the assertion.

### Before / After

```java
// BEFORE — no case for BOOTSTRAP_TOKEN, hits default -> AssertionError
switch (verb) {
    case READ_REPAIR:
    case MUTATION:
        return getWriteRpcTimeout();
    default:
        return getRpcTimeout();
}
```

```java
// AFTER
switch (verb) {
    case READ_REPAIR:
    case MUTATION:
        return getWriteRpcTimeout();
    case BOOTSTRAP_TOKEN:
        return BootStrapper.BOOTSTRAP_TIMEOUT;
    default:
        return getRpcTimeout();
}
```

---

## How to Spot

Every `switch` on an enum should handle all enum values, or at least have a clearly-safe `default`. When a new verb/enum value is added, search for all existing switch statements on that enum to ensure coverage.

---

## Test

No test added.
