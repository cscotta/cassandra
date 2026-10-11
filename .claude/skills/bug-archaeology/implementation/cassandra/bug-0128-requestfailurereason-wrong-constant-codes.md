# Bug 0128: RequestFailureReason Enum Constants Have Wrong Numeric Codes

**Commit**: bc40d3bb27 · **Ticket**: CASSANDRA-20126
**Subsystem**: src/java/org/apache/cassandra/exceptions
**Category**: Wrong constant / default value

---

## Root Cause

Four `RequestFailureReason` enum constants were assigned the wrong wire-protocol numeric codes. `READ_TOO_MANY_INDEXES` was given code 10 (the last position) while `NOT_CMS`, `INVALID_ROUTING`, and `COORDINATOR_BEHIND` were each shifted one position earlier than their correct values. Since these codes are serialized over the network and used by drivers to interpret error responses, any mismatch between a node that uses the wrong encoding and a client or peer that expects the correct one will produce misidentified failure reasons.

### Before / After

```java
// BEFORE
NOT_CMS                  (7),
INVALID_ROUTING          (8),
COORDINATOR_BEHIND       (9),
READ_TOO_MANY_INDEXES    (10),
```

```java
// AFTER
READ_TOO_MANY_INDEXES    (7),
NOT_CMS                  (8),
INVALID_ROUTING          (9),
COORDINATOR_BEHIND       (10),
```

---

## How to Spot

Whenever an enum carries explicit integer codes used for serialization, review the order in which constants are declared versus the order in which they were added to the protocol. A newly inserted constant that was placed after existing ones but assigned codes that displace later constants is the classic symptom. Adding a test that pins every (code, name) pair—so that any future reordering fails loudly—prevents regressions.

---

## Test

`RequestFailureReasonTest.testEnumCodesAndNames` iterates all enum values and asserts the expected (code, name) pair for each, also verifying the total count so new additions cannot silently shift existing codes.
