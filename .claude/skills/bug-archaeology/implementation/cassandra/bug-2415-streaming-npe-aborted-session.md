# Bug 2415: NPE when accessing StreamOutSession after receiver goes down during streaming

**Commit**: e0c655dc85 · **Ticket**: CASSANDRA-3904

## Detection Rule

When a streaming session can be asynchronously removed (e.g. because the remote peer failed), code that retrieves the session after the stream completes must null-check the result before dereferencing it.

## Code Smell

`StreamOutSession.get(to, header.sessionId).getFiles().size()` — chained call that NPEs if `get()` returns null, which it can when the session has been cancelled because the receiver died.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: nullable-session-lookup-deref
- severity: crash
- subsystem: streaming

## Root Cause

After `FileStreamTask.stream()` completes, the code called `StreamOutSession.get(to, header.sessionId)` to check whether all files had been sent. If the receiver node went down during streaming, the session would have been removed asynchronously, making `get()` return null. The subsequent `.getFiles().size()` call then threw a NPE. The fix assigns the result to a local variable and handles the null case with an informational log.

## Example

```java
// BEFORE
stream();
if (StreamOutSession.get(to, header.sessionId).getFiles().size() == 0)  // NPE if receiver died
    receiveReply();
```

```java
// AFTER
stream();
StreamOutSession session = StreamOutSession.get(to, header.sessionId);
if (session == null) {
    logger.info("Found no stream out session at end of file stream task - this is expected if the receiver went down");
} else if (session.getFiles().size() == 0) {
    receiveReply();
}
```
