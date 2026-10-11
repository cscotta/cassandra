# Bug 1700: Fix UnsupportedOperationException on CAS timeout

**Commit**: ad81cabdce · **Ticket**: CASSANDRA-6923
**Subsystem**: src/java/org/apache/cassandra/db/ConsistencyLevel.java
**Category**: Missing override of default method

---

## Root Cause

`ConsistencyLevel.blockFor()` handled the standard consistency levels (`ONE`, `QUORUM`, `ALL`, `LOCAL_QUORUM`, etc.) but had no cases for `SERIAL` and `LOCAL_SERIAL`. When a CAS (compare-and-set) operation timed out, the exception-handling path called `blockFor()` with a serial consistency level, hitting the `default` branch which threw `UnsupportedOperationException`. The fix adds `SERIAL` as a fall-through to the `QUORUM` case and `LOCAL_SERIAL` to the `LOCAL_QUORUM` case, since serial reads require the same quorum count.

### Before / After

```java
// BEFORE
case QUORUM:
    return quorumFor(keyspace);
// ...
case LOCAL_QUORUM:
    return localQuorumFor(keyspace, DatabaseDescriptor.getLocalDataCenter());
```

```java
// AFTER
case QUORUM:
case SERIAL:
    return quorumFor(keyspace);
// ...
case LOCAL_QUORUM:
case LOCAL_SERIAL:
    return localQuorumFor(keyspace, DatabaseDescriptor.getLocalDataCenter());
```

---

## How to Spot

Any `switch` on an enum without an exhaustive case list (or a meaningful `default`) is fragile: new enum values added later — or rarely exercised existing ones — will fall through to a crash. Require that `blockFor()` and similar dispatch methods explicitly enumerate every `ConsistencyLevel` value, or at minimum assert-fail on unknown values with a descriptive message.

---

## Test

No test added.
