# Bug 0738: Streaming race — sender thread starts before init message is sent

**Commit**: 06feaefba5 · **Ticket**: CASSANDRA-12886
**Subsystem**: Streaming
**Category**: Race condition / missing synchronization

---

## Root Cause
`ConnectionHandler.initiate()` called `MessageHandler.start()` (which spawns the sender/receiver thread) before calling `sendInitMessage()`. The receiving end could begin reading from the socket before the `StreamInitMessage` arrived, causing it to parse garbage or block indefinitely waiting for a protocol header that had not yet been written. The fix merges both steps: `sendInitMessage()` is now invoked inside `start()` when the `initiator` flag is true, guaranteeing the init message is flushed to the socket before the handler thread begins its read/write loop.

### Before / After
```java
// BEFORE — thread starts before message is written
incoming.start(incomingSocket, StreamMessage.CURRENT_VERSION);
incoming.sendInitMessage(incomingSocket, true);

// AFTER — init message written atomically inside start()
incoming.start(incomingSocket, StreamMessage.CURRENT_VERSION, true);
// sendInitMessage() is now called inside start() before Thread.start()
```

---

## How to Spot
Whenever a protocol requires a handshake message before data exchange, verify that the message is sent (and the write flushed) before the background thread is spawned. In review, look for patterns where `Thread.start()` and `socket.write()` appear in the same method and the write can be reordered after the thread is alive.

---

## Test
No new test added; the fix is structural — the race window is closed by moving the send inside the start method.
