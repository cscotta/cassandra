# Bug 2695: Race between add() and clear() in AdaptiveLatencyTracker

**Commit**: 0dc0029e82 · **Ticket**: CASSANDRA-2618

## Detection Rule
When a bounded queue's `offer()` fails and you attempt `remove()` to make room before retrying, catch `NoSuchElementException` because a concurrent `clear()` may have already emptied the queue between the failed `offer()` and the `remove()` call.

## Code Smell
`latencies.remove()` called unconditionally when `latencies.offer(i)` returns `false`, without guarding against `NoSuchElementException` thrown when a concurrent `clear()` empties the queue first.

## Tags
- scope: java
- category: race-condition
- code-pattern: toctou-queue-remove
- severity: crash
- subsystem: network-messaging

## Root Cause
`AdaptiveLatencyTracker.add()` tried to evict the head element before re-offering a new latency sample. A background thread concurrently calling `clear()` could drain the queue between the failed `offer()` and the `remove()` call, causing `remove()` to throw `NoSuchElementException`. The fix wraps the `remove()` call in a try-catch that silently discards the exception (the queue was already cleared, so there is nothing to remove).

## Example
```java
// BEFORE
if (!latencies.offer(i))
{
    latencies.remove();
    latencies.offer(i);
}
```
```java
// AFTER
if (!latencies.offer(i))
{
    try { latencies.remove(); }
    catch (NoSuchElementException e)
    { /* oops, clear() beat us to it */ }
    latencies.offer(i);
}
```
