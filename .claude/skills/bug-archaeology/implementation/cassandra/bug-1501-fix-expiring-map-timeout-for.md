# Bug 1501: Fix MS expiring map timeout for CAS messages

**Commit**: f7e8803345 · **Ticket**: CASSANDRA-7752
**Subsystem**: config/DatabaseDescriptor
**Category**: Wrong constant / default value

---

## Root Cause

`DatabaseDescriptor.getTimeout(MessagingService.Verb verb)` maps message verbs to their configured RPC timeout. The `PAXOS_PREPARE`, `PAXOS_PROPOSE`, and `PAXOS_COMMIT` verbs were not present in the switch statement, so they fell through to the `default` case which threw an `IllegalArgumentException`. In practice this caused the expiring map used by `MessagingService` to fail when computing the timeout for in-flight CAS (Paxos) messages, effectively breaking lightweight transactions.

### Before / After

```java
// BEFORE
case READ_REPAIR:
case MUTATION:
case COUNTER_MUTATION:
    return getWriteRpcTimeout();
```

```java
// AFTER
case READ_REPAIR:
case MUTATION:
case PAXOS_COMMIT:
case PAXOS_PREPARE:
case PAXOS_PROPOSE:
case COUNTER_MUTATION:
    return getWriteRpcTimeout();
```

---

## How to Spot

When adding new `Verb` enum constants, all switch statements that dispatch on `Verb` must be updated. A missing case silently falls to `default`; if `default` throws, the failure only surfaces at runtime when those messages are in flight.

---

## Test

No test added.
