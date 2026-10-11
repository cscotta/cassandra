# Bug 0308: Repair Verbs Use Generic RPC Timeout Instead of Dedicated Repair Timeout

**Commit**: f57c8d2c25 · **Ticket**: CASSANDRA-17557
**Subsystem**: src/java/org/apache/cassandra/net/Verb.java, src/java/org/apache/cassandra/config/Config.java
**Category**: Wrong constant / default value

---

## Root Cause

All repair-related message verbs (`VALIDATION_REQ/RSP`, `SYNC_REQ/RSP`, `PREPARE_MSG`, `CLEANUP_MSG`, and all incremental-repair verbs) were registered in the `Verb` enum with the generic `rpcTimeout` supplier instead of a dedicated repair timeout supplier. This meant that setting `repair_request_timeout_in_ms` (or its duration replacement) had no effect on when the messaging layer considered repair messages timed out; they all fell back to the standard read/write RPC timeout.

Additionally, `repair_request_timeout_in_ms` was declared as a raw `Long` in `Config` rather than a `SmallestDurationMilliseconds`, inconsistent with every other timeout field added under the new duration-based config system.

### Before / After

```java
// BEFORE – all repair verbs use generic rpcTimeout
VALIDATION_REQ  (101, P1, rpcTimeout, ANTI_ENTROPY, ...),
SYNC_REQ        (103, P1, rpcTimeout, ANTI_ENTROPY, ...),
// In Config:
public volatile Long repair_request_timeout_in_ms = 120000L;
// In VerbTimeouts: no repairTimeout constant existed
```

```java
// AFTER – repair verbs use a dedicated repairTimeout supplier
VALIDATION_REQ  (101, P1, repairTimeout, ANTI_ENTROPY, ...),
SYNC_REQ        (103, P1, repairTimeout, ANTI_ENTROPY, ...),
// In Config:
public volatile SmallestDurationMilliseconds repair_request_timeout = new SmallestDurationMilliseconds("120000ms");
// In VerbTimeouts:
static final ToLongFunction<TimeUnit> repairTimeout = DatabaseDescriptor::getRepairRpcTimeout;
```

---

## How to Spot

When adding a new timeout category to the config system, verify that every `Verb` enum entry for the new category actually references the new timeout supplier, not `rpcTimeout`. A quick grep for `rpcTimeout` in the `Verb` enum against the list of verbs belonging to the new category will catch this. Also confirm that any new config field follows the standard type (`SmallestDurationMilliseconds`) rather than a raw primitive.

---

## Test

No dedicated unit test added; the fix is structural (config type correction and verb timeout assignment).
