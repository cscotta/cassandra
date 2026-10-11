# Bug 1635: Fix NPE When Stream requestRanges

**Commit**: 60faac4340 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/streaming
**Category**: Off-by-one

---

## Root Cause

`StreamCoordinator.HostStreamingData.getOrCreateNextSession()` uses a round-robin index `lastReturned` to distribute work across a fixed pool of `StreamSession` objects. The index is reset to 0 when it reaches the last valid position. The original condition `lastReturned == streamSessions.size() - 1` only reset when the index equalled the last valid index, but `lastReturned` is incremented after each return (`lastReturned++`). If sessions are removed from the map or `lastReturned` somehow exceeded the map size, the equality check would never fire, allowing `lastReturned` to go out of bounds, causing `streamSessions.get(lastReturned++)` to return `null` and propagate an NPE to callers. Changing to `>=` ensures the index wraps on any out-of-range value.

### Before / After

```java
// BEFORE
if (lastReturned == streamSessions.size() - 1)
    lastReturned = 0;
return streamSessions.get(lastReturned++);
```

```java
// AFTER
if (lastReturned >= streamSessions.size() - 1)
    lastReturned = 0;
return streamSessions.get(lastReturned++);
```

---

## How to Spot

Round-robin index patterns that use equality to detect wrap-around are fragile: any path that increments the index by more than one, or that reduces the collection size, can skip past the sentinel value. Always use `>=` instead of `==` when guarding against an index going out of bounds.

---

## Test

No test added.
