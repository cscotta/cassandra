# Bug 2825: Race condition in MessagingService.targets using non-thread-safe Multimap

**Commit**: f4f5826338 · **Ticket**: CASSANDRA-1959

## Detection Rule
When a shared mutable collection is modified from multiple threads without synchronization, replace it with a thread-safe equivalent or use compare-and-set put semantics.

## Code Smell
`private static Multimap<String, InetAddress> targets` is an `ArrayListMultimap` (not thread-safe), but it is read and written concurrently by multiple messaging threads without any lock. Methods like `targets.put()` and `targets.removeAll()` are called without synchronization.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: unsynchronized-shared-collection
- severity: crash
- subsystem: network-messaging

## Root Cause
`ArrayListMultimap` is not thread-safe. Concurrent `put`, `removeAll`, and `remove` calls from multiple threads can corrupt the internal list, produce lost updates, or throw `ConcurrentModificationException`. The fix replaces it with a `NonBlockingHashMap<String, Collection<InetAddress>>` where each value is a `NonBlockingHashSet`, and uses `putIfAbsent` to safely initialize per-key collections.

## Example
```java
// BEFORE
private static Multimap<String, InetAddress> targets;
// in init:
targets = ArrayListMultimap.create();
// usage:
targets.put(messageId, endpoint);
Collection<InetAddress> addresses = targets.removeAll(messageId);

// AFTER
private static ConcurrentMap<String, Collection<InetAddress>> targets =
    new NonBlockingHashMap<>();
// usage:
putTarget(messageId, endpoint);  // uses putIfAbsent
Collection<InetAddress> addresses = targets.remove(messageId);
```
