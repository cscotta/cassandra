# Bug 1731: Timestamp Scaling Error in CAS Conditions

**Commit**: 5ef53e6f7c · **Ticket**: CASSANDRA-6623 (referenced in message)
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/CQL3CasConditions.java
**Category**: Wrong constant / default value

---

## Root Cause

`CQL3CasConditions` uses the current time to determine whether columns are live via `Column.isLive()`, which expects a timestamp in milliseconds. The query state timestamp (`queryState.getTimestamp()`) is in microseconds. An earlier attempted fix multiplied the already-microsecond value by 1000, making it even larger and pushing it even further from the expected millisecond range. The correct fix divides the microsecond timestamp by 1000 on the way into `CQL3CasConditions`, aligning its stored `now` field with what `Column.isLive()` requires.

### Before / After

```java
// BEFORE (in ModificationStatement)
// comment claimed "query timestamp is in seconds" — wrong, it's microseconds
CQL3CasConditions conditions = new CQL3CasConditions(cfm, queryState.getTimestamp() * 1000);
```

```java
// AFTER: pass microseconds directly; CQL3CasConditions divides internally
// In ModificationStatement:
CQL3CasConditions conditions = new CQL3CasConditions(cfm, queryState.getTimestamp());

// In CQL3CasConditions constructor:
this.now = now / 1000; // convert microseconds → milliseconds for Column.isLive()
```

---

## How to Spot

Whenever a timestamp is passed across an API boundary, verify the units at both ends. A mismatch comment (e.g., "in seconds" when the actual unit is microseconds) is a strong signal that the unit assumption was never validated. Centralise the conversion at the point of storage so that callers cannot apply it inconsistently.

---

## Test

No test added.
