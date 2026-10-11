# Bug 2191: Duplicate SSTable reference accumulated when stream session failed mid-transfer

**Commit**: d401d9f1de · **Ticket**: CASSANDRA-3306
**Subsystem**: src/java/org/apache/cassandra/streaming/IncomingStreamReader.java, src/java/org/apache/cassandra/streaming/StreamInSession.java
**Category**: State not cleaned up

---

## Root Cause

`StreamInSession.get(host, sessionId)` always created a new session if one did not exist. When a streaming session failed partway through (after the first file was received), the session was removed from the session map. If the sender then retried by sending the next file header (which carries an empty `pendingFiles` list), `get` would silently create a fresh session for the same session ID. This orphaned session accumulated SSTable references that were never released, causing a duplicate-reference leak and potentially preventing SSTables from being deleted. The fix adds a `hasSession` check in `IncomingStreamReader`: if `pendingFiles` is empty and the indicated session no longer exists, the receiver immediately sends a `SESSION_FAILURE` reply and throws an `IOException` rather than creating a ghost session.

### Before / After

```java
// BEFORE — always creates session, even for a dead/retried session
session = StreamInSession.get(host, header.sessionId);
```

```java
// AFTER — guard against re-creating a closed session
if (header.pendingFiles.isEmpty() && header.file != null) {
    if (!StreamInSession.hasSession(host, header.sessionId)) {
        // send failure and abort
        throw new IOException("Session " + header.sessionId + " already closed.");
    }
}
session = StreamInSession.get(host, header.sessionId);
```

---

## How to Spot

Any "get-or-create" session factory method is a risk for state leaks when sessions can be interrupted mid-stream. If the protocol sends multiple messages belonging to the same session, the receiver must verify the session is still valid before creating or resuming it, especially when the first message in a retry would not carry enough context to distinguish a new session from a resumed dead one.

---

## Test

No test added.
