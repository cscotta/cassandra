# Bug 2651: Hadoop ColumnFamilyRecordWriter loses exceptions — close() checks lastException before thread joins

**Commit**: 57684e9aab · **Ticket**: CASSANDRA-2755

## Detection Rule
When a background writer thread records exceptions in a `volatile` field and the caller must propagate them, always join the thread before checking `lastException`; checking before join means the exception may not yet have been recorded, and exceptions thrown during join-phase cleanup are swallowed.

## Code Smell
`RangeClient.stopNicely()` checks `if (lastException != null) throw lastException` before setting `run=false` and interrupting the thread; if the exception is recorded after the check but before the join, it is never propagated; also the caller catches `InterruptedException` from join inside a loop and swallows all but the last per-client exception.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: lastexception-checked-before-join
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`ColumnFamilyRecordWriter.close()` iterated clients calling `client.stopNicely()` which set `run=false`, interrupted the thread, and then (in a separate loop) called `client.join()` followed by `client.close()`. The check for `lastException` in `stopNicely()` happened before the thread was joined, so if the exception was set concurrently after the check, it was never propagated. Additionally, exceptions from individual client closes were not accumulated — the code could throw from the join loop before closing remaining clients. The fix restructures to: stop the thread, join it, then check `lastException`, and accumulates the first exception across all clients before throwing.

## Example
```java
// BEFORE
public void stopNicely() throws IOException {
    if (lastException != null) throw lastException;  // race: may miss exception
    run = false; interrupt();
}

// AFTER
public void close() throws IOException {
    run = false; interrupt();
    this.join();           // wait for thread to finish
    if (lastException != null) throw lastException;  // now safe to check
}
```
