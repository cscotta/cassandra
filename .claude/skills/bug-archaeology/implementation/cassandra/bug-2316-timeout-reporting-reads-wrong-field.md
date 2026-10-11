# Bug 2316: Timeout reporting to dynamic snitch uses message's timeout field instead of the map entry's actual timeout

**Commit**: f422b98140 · **Ticket**: CASSANDRA-2819

## Detection Rule

When an expiry callback receives a wrapped cache entry, verify that timeout/latency values are read from the wrapper (which recorded them at insertion time) rather than reconstructed by calling methods on the unwrapped value object, which may no longer reflect the actual timeout used.

## Code Smell

In a post-expire hook, reading timeout as `expiredCallbackInfo.sentMessage.getTimeout()` from the unwrapped value rather than from the expiry wrapper's own `timeout` field — the two values may differ if messages use per-message timeouts.

## Tags

- scope: cassandra
- category: wrong-serialization
- code-pattern: wrong-field-from-unwrapped-value
- severity: performance
- subsystem: network-messaging

## Root Cause

`MessagingService`'s timeout reporter callback was defined with a `Function<Pair<String, CallbackInfo>, ?>` signature, but `ExpiringMap`'s post-expire hook actually passes `Pair<K, CacheableObject<V>>` — a pair of key plus the wrapper that recorded the expiry time and timeout. The code was calling `.getValue()` to unwrap to `CallbackInfo` and then reading `.sentMessage.getTimeout()`, which returned the message's nominal timeout rather than the actual time the entry was supposed to expire. The fix makes `CacheableObject` public, changes the hook signature to expose the wrapper, and reads `pair.right.timeout` directly.

## Example

```java
// BEFORE: callback receives unwrapped value, reads wrong timeout
Function<Pair<String, CallbackInfo>, ?> timeoutReporter = pair -> {
    CallbackInfo expiredCallbackInfo = pair.right;
    maybeAddLatency(expiredCallbackInfo.callback, expiredCallbackInfo.target,
                    (double) expiredCallbackInfo.sentMessage.getTimeout());  // wrong source
};
```

```java
// AFTER: callback receives wrapper, reads the timeout stored at put() time
Function<Pair<String, ExpiringMap.CacheableObject<CallbackInfo>>, ?> timeoutReporter = pair -> {
    CallbackInfo expiredCallbackInfo = pair.right.getValue();
    maybeAddLatency(expiredCallbackInfo.callback, expiredCallbackInfo.target,
                    (double) pair.right.timeout);  // correct: actual expiry timeout
};
```
