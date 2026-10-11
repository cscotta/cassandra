# Bug 2666: ConcurrentModificationException when removing endpoints during repair

**Commit**: 1e77d805fb · **Ticket**: CASSANDRA-2767

## Detection Rule
When iterating over a collection and removing elements that match a predicate, use an iterator's `remove()` rather than the collection's `remove()` to avoid ConcurrentModificationException.

## Code Smell
A `for-each` loop over a mutable collection that calls `collection.remove(element)` inside the loop body instead of `iterator.remove()`.

## Tags
- scope: java
- category: wrong-filter-result
- code-pattern: for-each-collection-remove
- severity: crash
- subsystem: repair

## Root Cause
The `getRepairNeighbors()` method iterated over a `neighbors` collection using a for-each loop and called `neighbors.remove(endpoint)` directly, which modifies the underlying collection mid-iteration and throws `ConcurrentModificationException`. The fix switches to an explicit `Iterator` and calls `iter.remove()` instead.

## Example
```java
// BEFORE
for (InetAddress endpoint : neighbors)
{
    if (Gossiper.instance.getVersion(endpoint) <= MessagingService.VERSION_07)
        neighbors.remove(endpoint);
}
```
```java
// AFTER
Iterator<InetAddress> iter = neighbors.iterator();
while (iter.hasNext())
{
    InetAddress endpoint = iter.next();
    if (Gossiper.instance.getVersion(endpoint) <= MessagingService.VERSION_07)
        iter.remove();
}
```
