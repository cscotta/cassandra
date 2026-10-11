# Bug 1145: Premature MATERIALIZED_VIEW_MUTATION Verb Wires Up Unused Infrastructure

**Commit**: 39ee04088c · **Ticket**: CASSANDRA-9993
**Subsystem**: src/java/org/apache/cassandra/net/MessagingService.java
**Category**: Wrong constant / default value

---

## Root Cause

`Verb.MATERIALIZED_VIEW_MUTATION` was added to the `Verb` enum before its feature was ready, and it was wired into every critical messaging table: verb-to-stage routing, serializer map, response-serializer map, droppable-verb set, timeout lookup, and verb-handler registration. Because `Verb` ordinals are used for wire serialization, having an unimplemented, incorrectly-placed verb in the enum could cause ordinal mismatches and misrouted messages. The fix removes all live wiring for the premature verb and replaces its reserved ordinal slot with `UNUSED_4`/`UNUSED_5` placeholders to preserve the ordinal sequence of all subsequent verbs.

### Before / After

```java
// BEFORE – MATERIALIZED_VIEW_MUTATION appears in the active verb enum and all routing tables
BATCHLOG_MUTATION,
MATERIALIZED_VIEW_MUTATION,
// remember to add new verbs at the end
UNUSED_1,
UNUSED_2,
UNUSED_3,
```

```java
// AFTER – slot retired to UNUSED entries; verb removed from all routing maps
BATCHLOG_MUTATION,
// remember to add new verbs at the end
UNUSED_1,
UNUSED_2,
UNUSED_3,
UNUSED_4,
UNUSED_5,
```

---

## How to Spot

Any new `Verb` wired into serializer maps, stage routing, and droppable sets before the corresponding feature code exists is a hazard. Reviewers should check that a new verb is only registered once its handler, serializer, and stage are all implemented, and that any premature verb occupies an `UNUSED_N` placeholder so ordinals remain stable for in-flight upgrades.

---

## Test

No test added.
