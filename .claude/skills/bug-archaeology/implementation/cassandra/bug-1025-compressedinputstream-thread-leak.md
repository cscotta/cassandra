# Bug 1025: CompressedInputStream Reader Thread Leaks on Close

**Commit**: 8b9a9161ca · **Ticket**: CASSANDRA-10012
**Subsystem**: Streaming / compression
**Category**: Resource leak

---

## Root Cause
`CompressedInputStream` spawned a background `Reader` thread to fill a `BlockingQueue` with decompressed data chunks, but had no `close()` method and no way to interrupt that thread. If the consumer stream was closed early (e.g. due to an error during streaming), the background thread would block indefinitely on `dataBuffer.put()` or `source.read()`, leaking the thread and the socket connection. Additionally, `CompressedStreamReader` never called `close()` on the `CompressedInputStream`, so the reader thread was never cleaned up even in the normal-termination path.

### Before / After
```java
// BEFORE — no close(), thread runs forever; Reader is a static class
// with no reference back to the outer stream
static class Reader extends WrappedRunnable { ... }

// Constructor spawned but never stored the thread reference
new Thread(new Reader(source, info, dataBuffer)).start();

// AFTER — thread stored, close() interrupts it; Reader becomes inner class
private Thread readerThread;
readerThread = new Thread(new Reader(source, info, dataBuffer));
readerThread.start();

@Override
public void close() throws IOException {
    synchronized(this) {
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
    }
}

// Reader loop now checks interrupt flag
while (!Thread.currentThread().isInterrupted() && chunks.hasNext()) { ... }

// CompressedStreamReader.read() now has a finally block
finally {
    cis.close();
}
```

---

## How to Spot
Any class that spawns a background thread must store a reference to it and provide a `close()` / `stop()` method that interrupts or joins it. During review, look for `new Thread(...).start()` where the result is discarded and no corresponding lifecycle method exists. Also verify that every caller of a closeable stream wraps it in try-with-resources or has a `finally { stream.close(); }` block.

---

## Test
`CompressedInputStreamTest.testClose()` was added: it creates a `CompressedInputStream` backed by a blocking input stream, closes it from a separate thread, then verifies the main `read()` call unblocks and throws `EOFException` rather than hanging.
