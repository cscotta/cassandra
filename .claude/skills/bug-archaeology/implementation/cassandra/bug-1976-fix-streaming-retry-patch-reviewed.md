# Bug 1976: Streaming retry broken — no ACK message and no stream drain on failure

**Commit**: dfc9faf28b · **Ticket**: CASSANDRA-5775
**Subsystem**: streaming/StreamReader.java, streaming/StreamSession.java, streaming/messages/
**Category**: Logic error in condition

---

## Root Cause

The streaming retry mechanism was broken in two ways. First, when a receiver successfully received a file, it never sent an acknowledgement back to the sender, so `StreamTransferTask.complete()` was never called and the sender had no way to know the transfer succeeded or to trigger a retry. Second, when reading a file failed partway through, the partially-read bytes were not drained from the channel before any retry, leaving the stream in a corrupt state for subsequent messages.

The fix introduces a new `RECEIVED` message type that the receiver sends back to the sender after successfully writing an SSTable. On error, `StreamReader.drain()` skips the remaining bytes so the underlying channel stays in sync.

### Before / After

```java
// BEFORE: receiver never sent ACK; sender's complete() was called from onFileSent, not on ACK
public void received(FileMessage message) {
    StreamingMetrics.totalIncomingBytes.inc(message.header.size());
    metrics.incomingBytes.inc(message.header.size());
    receivers.get(message.header.cfId).received(message.sstable);
}

// No drain on error:
catch (Throwable e) {
    writer.abort();
    if (e instanceof IOException) throw (IOException) e;
    ...
}
```

```java
// AFTER: send RECEIVED ACK back to sender
public void receive(FileMessage message) {
    StreamingMetrics.totalIncomingBytes.inc(message.header.size());
    metrics.incomingBytes.inc(message.header.size());
    handler.sendMessage(new ReceivedMessage(message.header.cfId, message.header.sequenceNumber));
    receivers.get(message.header.cfId).received(message.sstable);
}

// Drain remaining bytes on error so channel stays in sync:
catch (Throwable e) {
    writer.abort();
    drain(dis, in.getBytesRead());
    ...
}
```

---

## How to Spot

When a protocol involves request/reply, check that both sides of the exchange are implemented: the sender must wait for confirmation and the receiver must send it. Also verify that on partial reads or write failures the input stream is fully consumed before the connection is reused.

---

## Test

No test added.
