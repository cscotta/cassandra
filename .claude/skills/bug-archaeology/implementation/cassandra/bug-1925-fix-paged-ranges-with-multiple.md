# Bug 1925: PAGED_RANGE verb registered with wrong serializer and no verb handler on replicas

**Commit**: a2824e63a6 · **Ticket**: CASSANDRA-6004
**Subsystem**: src/java/org/apache/cassandra/net/MessagingService.java, src/java/org/apache/cassandra/service/StorageService.java
**Category**: Wrong constant / default value

---

## Root Cause

Two separate omissions combined to break paged range queries when they were forwarded to replica nodes. First, `MessagingService` registered `Verb.PAGED_RANGE` with `RangeSliceCommand.serializer` instead of the correct `PagedRangeCommand.serializer`, causing deserialization failures on the receiving node. Second, `StorageService` never registered a verb handler for `PAGED_RANGE`, so even if deserialization had succeeded, replicas would have had no handler to process the incoming request.

### Before / After

```java
// BEFORE (MessagingService.java)
put(Verb.PAGED_RANGE, RangeSliceCommand.serializer);
```

```java
// AFTER (MessagingService.java)
put(Verb.PAGED_RANGE, PagedRangeCommand.serializer);
```

```java
// BEFORE (StorageService.java) — PAGED_RANGE handler missing entirely
MessagingService.instance().registerVerbHandlers(Verb.RANGE_SLICE, new RangeSliceVerbHandler());
// PAGED_RANGE not registered
```

```java
// AFTER (StorageService.java)
MessagingService.instance().registerVerbHandlers(Verb.RANGE_SLICE, new RangeSliceVerbHandler());
MessagingService.instance().registerVerbHandlers(Verb.PAGED_RANGE, new RangeSliceVerbHandler());
```

---

## How to Spot

When adding a new verb to `MessagingService`, verify that both the serializer registration in `MessagingService` and the verb handler registration in `StorageService` are updated together. Missing either half silently breaks the feature only in multi-replica clusters where the coordinator must forward to remote nodes.

---

## Test

No test added.
