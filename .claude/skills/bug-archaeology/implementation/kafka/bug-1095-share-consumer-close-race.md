## Commit

58c08441d1 — KAFKA-19229: Share consumer close hangs when background error arrives after unsubscribe

## Ticket

KAFKA-19229

## Subsystem

Share groups / consumer close

## Category

Race condition / missing synchronization

## Root cause

`ShareConsumerImpl.close()` called `handleCompletedAcknowledgements()` to drain the background event queue before shutting down. If a background error event (e.g., `GroupAuthorizationException`) arrived after `unsubscribe()` was called but before the drain completed, `handleCompletedAcknowledgements` would re-throw the exception, preventing `close()` from completing and leaving the consumer in a half-closed state. The root cause is that error events queued during normal operation are no longer actionable once the consumer has started closing, so throwing them during close is wrong.

## Before / After

```java
// Before — always throws background exceptions, blocks close()
private void handleCompletedAcknowledgements() {
    processBackgroundEvents();  // throws on any error event
}

// After — ignores auth/topic exceptions during close
private void handleCompletedAcknowledgements(boolean onClose) {
    processBackgroundEvents(onClose
        ? e -> (e instanceof GroupAuthorizationException
             || e instanceof TopicAuthorizationException
             || e instanceof InvalidTopicException)
        : e -> false);
}
```

## How to spot

In `close()` methods that drain an event queue, check whether exceptions from that queue are re-thrown. Any re-thrown exception from a background queue will prevent `close()` from completing if the exception arrives after the consumer has already logically disconnected. Suppress non-fatal background errors during close.

## Test

`ShareConsumerImplTest` was extended with a test that injects a `GroupAuthorizationException` background event after `unsubscribe()` and verifies that `close()` completes without throwing.
