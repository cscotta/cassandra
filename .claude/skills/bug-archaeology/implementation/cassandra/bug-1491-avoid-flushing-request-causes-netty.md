# Bug 1491: Avoid Flushing on Request Threads — Causes Netty to Block

**Commit**: f2d60577c3 · **Ticket**: CASSANDRA-7743
**Subsystem**: src/java/org/apache/cassandra/transport
**Category**: Resource leak

---

## Root Cause

In `Message.Dispatcher.channelRead0()`, after building the response the code called `ctx.writeAndFlush()` directly on the Netty I/O (request) thread for error paths, and queued normal responses for the `Flusher` coalescing loop. For error paths the `ctx.writeAndFlush()` call on the I/O thread caused Netty's internal buffer pool to accumulate unpooled ByteBuffers over time because the direct flush bypassed the coalescing path that also handles releasing the source `Frame`. The source frame was released inconsistently: normal responses released it via `Flusher` but error responses released it via `request.getSourceFrame().release()` before the write, meaning if the write was reordered the release could happen too early. The fix routes both normal and error responses through a single `flush(FlushItem)` helper that always enqueues to the `Flusher`, ensuring the source frame is always released after the coalesced write.

### Before / After

```java
// BEFORE (error path flushed directly on I/O thread)
catch (Throwable ex)
{
    request.getSourceFrame().release();
    ctx.writeAndFlush(ErrorMessage.fromException(ex).setStreamId(request.getStreamId()), ctx.voidPromise());
    return;
}
// normal path: queued to Flusher but sourceFrame stored on response object
response.setSourceFrame(request.getSourceFrame());
flusher.queued.add(new FlushItem(ctx, response));
```

```java
// AFTER (both paths go through flush() helper)
catch (Throwable ex)
{
    flush(new FlushItem(ctx, ErrorMessage.fromException(ex).setStreamId(request.getStreamId()), request.getSourceFrame()));
    return;
}
flush(new FlushItem(ctx, response, request.getSourceFrame()));

private void flush(FlushItem item)
{
    EventLoop loop = item.ctx.channel().eventLoop();
    // ... enqueue to Flusher ...
    flusher.queued.add(item);
}
```

---

## How to Spot

In Netty pipelines, every `Frame` (or equivalent pooled buffer) that enters the pipeline needs exactly one release after the write is done. When error and success paths use different write mechanisms, check that both paths perform the matching release — and that neither releases the buffer before the write completes.

---

## Test

No test added.
