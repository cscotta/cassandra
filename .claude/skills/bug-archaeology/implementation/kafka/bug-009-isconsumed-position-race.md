**Commit**: 5d03ccff57 · **Ticket**: KAFKA-15529
**Subsystem**: clients — CompletedFetch / FetchCollector
**Category**: Race condition / missing synchronization

**Root cause**
In the async consumer fetch path, `CompletedFetch.drain()` set `isConsumed = true` *before* the subscription position was updated. Because `isConsumed` was not `volatile`, the background thread could observe a stale `true` value and simultaneously see the old, un-advanced position, causing it to issue a duplicate fetch request for the already-consumed offset. The fix reorders operations: position is updated first in `FetchCollector`, then `drain()` is called after position advancement; and `isConsumed` is declared `volatile` so the write is immediately visible across threads.

**Before/After**
```java
// Before — drain() (sets isConsumed=true) called before position update
if (...exhausted...) {
    drain();   // isConsumed=true written before position advance
    return null;
}
// position updated after this method returns

// After — exhausted flag set, drain deferred until after position update
exhausted = true;   // local flag, no visibility yet
// ... position is updated in FetchCollector ...
if (nextInLineFetch.isExhausted()) {
    nextInLineFetch.drain();   // isConsumed=true only after position advanced
}
```

**How to spot**
When a flag is used as a signal to another thread that "this resource is done and a new request can be issued", the flag must be set *after* all associated state (positions, offsets) has been fully updated. A non-`volatile` boolean used for inter-thread visibility is also a red flag. Search for `drain()` / `isConsumed` in the fetch path and verify ordering relative to position writes.

**Test**
`FetchCollectorTest#testPositionUpdatedBeforeDrainOnExhaustedFetch` — spies on `drain()` and captures the subscription position at drain time, then asserts the position was already advanced before `drain()` was called.
