# Bug 2069: Streaming Compressed Files Skips Seek and Omits Flush When Encryption Is Active

**Commit**: cc6429e272 · **Ticket**: CASSANDRA-5391
**Subsystem**: src/java/org/apache/cassandra/streaming/compress/CompressedFileStreamTask.java
**Category**: Logic error in condition

---

## Root Cause

`CompressedFileStreamTask` has two code paths for sending compressed sections: an NIO fast path (using `SocketChannel`) and a fallback byte-copy path used when inter-node encryption is enabled (because NIO is unavailable with TLS). The fallback path was missing two operations that the NIO path handled implicitly. First, it never called `file.seek(section.left)` before reading, so it could start reading from the wrong file offset when handling sections that do not begin at the current position. Second, after writing all sections it never flushed the plain `OutputStream`, so the last partial buffer could be silently discarded by the socket layer. Both omissions caused data corruption or incomplete file transfers when streaming with encryption.

### Before / After

```java
// BEFORE – no seek and no flush in fallback path
else
{
    // NIO not available (encryption enabled)
    if (transferBuffer == null)
        transferBuffer = new byte[DEFAULT_CHUNK_SIZE];
    // ... byte copy loop ...
}
// end of sections loop – no flush
```

```java
// AFTER – seek before reading, flush after all sections
else
{
    file.seek(section.left);  // added
    if (transferBuffer == null)
        transferBuffer = new byte[DEFAULT_CHUNK_SIZE];
    // ... byte copy loop ...
}

if (sc == null)
    socket.getOutputStream().flush();  // added
```

---

## How to Spot

Whenever a fallback code path mirrors a primary path that uses a stateful resource (file position, buffered output stream), verify that the fallback also handles all the stateful transitions the primary path performs implicitly. Divergent paths around a resource are a common site for missing seek, flush, or close calls.

---

## Test

No test added.
