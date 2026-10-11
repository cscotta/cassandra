# Bug 2993: Gossiper eviction duration computed as current time instead of elapsed time, evicting dead endpoints instantly

**Commit**: 589d72a7f9 · **Ticket**: n/a

## Detection Rule
When computing elapsed time since an event, write `currentTime - eventTime` rather than `currentTime - (currentTime - eventTime)`, which trivially equals `eventTime` and bears no relationship to elapsed duration.

## Code Smell
Two variables `now` and `l = now - timestamp` are computed, then `duration = now - l` is calculated; substituting shows `duration = now - (now - timestamp) = timestamp`, so duration equals the raw timestamp value rather than elapsed milliseconds.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: duration-equals-timestamp
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
`Gossiper.doStatusCheck()` computed `l = now - epState.getUpdateTimestamp()` (correctly the age) then computed `duration = now - l`. Algebraically, `duration = now - (now - timestamp) = timestamp`, so the "duration" was the raw epoch millisecond of the last update rather than how long ago the update occurred. For any node updated even once since epoch, `duration` was enormous, causing `duration > aVeryLongTime_` to be trivially true and all dead endpoints to be evicted immediately. The fix removes the intermediate variable and computes `duration = System.currentTimeMillis() - epState.getUpdateTimestamp()` directly.

## Example
```java
// BEFORE: algebraically wrong — duration equals the timestamp, not elapsed time
long now = System.currentTimeMillis();
long l = now - epState.getUpdateTimestamp();
long duration = now - l; // = now - (now - ts) = ts  ← BUG

// AFTER: correct elapsed time
long duration = System.currentTimeMillis() - epState.getUpdateTimestamp();
```
