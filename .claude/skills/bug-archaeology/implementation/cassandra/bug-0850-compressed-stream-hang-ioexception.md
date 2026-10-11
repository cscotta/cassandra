# Bug 0850: Compressed Stream Reader Hangs on IOException

**Commit**: 76e3100ffb · **Ticket**: CASSANDRA-10992
**Subsystem**: Streaming
**Category**: Race condition / missing synchronization

---

## Root Cause
`CompressedInputStream` uses a producer–consumer queue (`dataBuffer`) where a background `Reader` thread puts compressed chunks for the foreground deserialization thread to consume.  When the `Reader` encountered an `IOException` it placed a sentinel `POISON_PILL` byte array in the queue, but did not store the exception anywhere.  The consumer's `read()` method, upon receiving `POISON_PILL`, threw a generic `EOFException("No chunk available")` rather than re-throwing the original error, and in some code paths it called `drain()` before re-throwing — a blocking operation that could never complete because the producer had already exited.  The result was a stream session that hung indefinitely instead of failing fast.  The fix stores the `IOException` (or synthesised `EOFException`) in a `volatile` field on `CompressedInputStream` before placing `POISON_PILL`, and removes the blocking `drain()` call from the error path in `CompressedStreamReader`.

### Before / After
```java
// BEFORE – Reader puts POISON_PILL but stores no exception; consumer throws generic EOFException
if (r < 0) {
    dataBuffer.put(POISON_PILL);
    return;
}
// In CompressedStreamReader error handler:
drain(cis, in.getBytesRead());   // ← could block forever
if (e instanceof IOException) throw (IOException) e;

// AFTER – Reader stores exception before POISON_PILL; consumer re-throws it directly
if (r < 0) {
    readException = new EOFException("No chunk available");
    dataBuffer.put(POISON_PILL);
    return;
}
// In CompressedStreamReader error handler:
if (extractIOExceptionCause(e).isPresent()) throw e;
throw Throwables.propagate(e);
```

---

## How to Spot
In producer–consumer designs where a sentinel signals an error, the exception must be communicated out-of-band (e.g., a shared volatile field) before the sentinel is enqueued.  If the consumer derives the exception solely from the sentinel value itself, information is lost and blocking post-error cleanup code can deadlock.  Also watch for blocking calls (`drain`, `join`, `take`) that appear in exception handlers — they are latent hangs.

---

## Test
`CompressedInputStreamTest` extended with cases that inject `IOException` from the source stream and verify the consumer throws promptly rather than blocking.
