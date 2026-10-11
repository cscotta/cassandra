**Commit**: 70be719f6a · **Ticket**: KAFKA-19950

**Subsystem**: core/src/main/java/kafka/server/share/session/ShareSessionCache.java

**Category**: State not cleaned up

**Root cause**
When a client reconnected and sent a ShareFetch request with `initial epoch`, the broker created a new `ShareSession` and evicted the old one from the cache. However, the old session's entry in the connection-listener map was not marked stale before eviction, so when the subsequent network disconnection event fired for the old connection ID, the broker mistakenly triggered a member-leave event for the group. Marking the old session as stale before removing it from the cache prevents the disconnection event handler from acting on a session that has already been superseded.

**Before**
```java
// Before: old session removed without marking stale
ShareSession oldSession = connectionListenerMap.remove(connectionId);
cache.remove(oldSession.key());
```

**After**
```java
// After: mark stale first so disconnect handler is a no-op
oldSession.markStale();
ShareSession oldSession = connectionListenerMap.remove(connectionId);
cache.remove(oldSession.key());
```

**How to spot**
Look for patterns where a session object is removed from a listener map without first being invalidated; any subsequent asynchronous event that checks the same connection ID will find the map empty and may fire a spurious leave or error. The `ShareSession` constructor should also now accept a `connectionId` to tighten the association.

**Test**
Write a test that creates a session, forces a reconnect with initial epoch, and then fires a disconnection event, asserting that no member-leave is triggered for the group.
