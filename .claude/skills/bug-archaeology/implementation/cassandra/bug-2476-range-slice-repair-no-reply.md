# Bug 2476: Range slice repair handler sends no reply, causing read timeout at coordinator

**Commit**: fc42891a3a · **Ticket**: CASSANDRA-3551

## Detection Rule

When a verb handler applies a mutation but never sends a reply back to the coordinator, any read-repair path that expects an acknowledgment will time out waiting forever.

## Code Smell

A `VerbHandler.doVerb()` implementation that deserializes and applies a mutation with no `MessagingService.instance().sendReply(...)` call — the handler silently succeeds with no response.

## Tags

- scope: cassandra
- category: silent-skip
- code-pattern: missing-verb-handler-reply
- severity: hang
- subsystem: storage-engine

## Root Cause

`ReadRepairVerbHandler.doVerb()` applied the row mutation but never sent a `WriteResponse` reply to the originating coordinator. The coordinator would block waiting for acknowledgments from all replicas involved in the read-repair, causing read timeouts. The fix adds reply construction and `sendReply` after the mutation is applied.

## Example

```java
// BEFORE
RowMutation rm = RowMutation.serializer().deserialize(...);
rm.apply();
// no reply sent
```

```java
// AFTER
RowMutation rm = RowMutation.serializer().deserialize(...);
rm.apply();

WriteResponse response = new WriteResponse(rm.getTable(), rm.key(), true);
Message responseMessage = WriteResponse.makeWriteResponseMessage(message, response);
MessagingService.instance().sendReply(responseMessage, id, message.getFrom());
```
