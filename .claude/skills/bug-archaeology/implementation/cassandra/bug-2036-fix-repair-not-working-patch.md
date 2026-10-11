# Bug 2036: SNAPSHOT Verb Handler Missing From MessagingService Serializer Map

**Commit**: 68444f9b2f · **Ticket**: CASSANDRA-5512
**Subsystem**: src/java/org/apache/cassandra/net/MessagingService.java
**Category**: Silent skip on non-match

---

## Root Cause

`MessagingService` maintains a static map of verb-to-serializer entries used to route incoming network messages. The `SNAPSHOT` verb was never added to this map, so when a repair with `--snapshot` triggered a remote snapshot command, `MessagingService` had no serializer registered for it. The verb was silently skipped or caused a lookup failure, making the snapshot phase of repair a no-op on remote nodes.

### Before / After

```java
// BEFORE
put(Verb.COUNTER_MUTATION, CounterMutation.serializer);
// SNAPSHOT verb absent
```

```java
// AFTER
put(Verb.COUNTER_MUTATION, CounterMutation.serializer);
put(Verb.SNAPSHOT, SnapshotCommand.serializer);
```

---

## How to Spot

When adding a new network verb, verify that its serializer is registered in the `MessagingService` verb-to-serializer map. A missing entry is not a compile-time error and will only manifest at runtime when the verb is exercised over the network.

---

## Test

No test added.
