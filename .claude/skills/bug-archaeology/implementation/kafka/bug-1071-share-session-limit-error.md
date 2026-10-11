## Commit

f9064f8bcd — KAFKA-19231-1: Wrong error code and missing delay when share session cache is full

## Ticket

KAFKA-19231

## Subsystem

Share groups / share session management

## Category

Wrong constant / default value

## Root cause

When the share session cache was full, `SharePartitionManager` returned `SHARE_SESSION_NOT_FOUND` to the client. The correct error is `SHARE_SESSION_LIMIT_REACHED`, which has different client retry semantics — clients that receive `NOT_FOUND` would re-issue the request immediately in a tight loop instead of backing off. Additionally, no delay was introduced before the error response was sent; the response was dispatched synchronously, amplifying the retry storm. The fix throws `ShareSessionLimitReachedException` (which maps to `SHARE_SESSION_LIMIT_REACHED`) and queues the response through an `IdleShareFetchTimerTask` to enforce the `maxWaitMs` back-pressure delay.

## Before / After

```java
// Before — wrong error, no delay
if (shareSessionCache.isFull()) {
    return ShareFetchResponse.error(Errors.SHARE_SESSION_NOT_FOUND);
}

// After — correct error code, with maxWaitMs delay
if (shareSessionCache.isFull()) {
    throw new ShareSessionLimitReachedException();
    // response sent via IdleShareFetchTimerTask after maxWaitMs
}
```

## How to spot

Search for places where a session-cache or connection-limit guard returns a `NOT_FOUND` error. When the correct semantic is "limit exceeded" or "try again later," using `NOT_FOUND` misleads the client into immediate retry rather than backoff, creating a retry storm under load.

## Test

`SharePartitionManagerTest` was extended with a test that verifies `SHARE_SESSION_LIMIT_REACHED` is returned and that the response is not sent before `maxWaitMs` elapses.
