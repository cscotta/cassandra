# Bug 2794: SSTable Non-Data Component Deletion Skips Files When DATA Component Already Removed

**Commit**: 90e8804666 · **Ticket**: CASSANDRA-2059

## Detection Rule
When code mutates a live collection (calls `Set.remove()` on it) and then passes the same collection to a method that iterates it, the mutation may not be visible if the iteration was already underway or the collection is a view — use a derived view instead.

## Code Smell
`components.remove(Component.DATA)` modifies the set in-place before passing it to `SSTable.delete(desc, components)`. If `components` is an unmodifiable or reference-shared set, `remove()` throws; if it succeeds but `delete()` received the same reference already filtered, a subtle over-delete or under-delete can occur depending on implementation.

## Tags
- scope: java
- category: state-not-cleaned
- code-pattern: mutate-then-pass-same-reference
- severity: data-loss
- subsystem: storage-engine

## Root Cause
`SSTableDeletingReference.cleanup()` needed to delete all SSTable components except the data file (which was already deleted earlier). The original code called `components.remove(Component.DATA)` on the live set and then passed the same set to `SSTable.delete()`. This mutated the canonical component set in place, which could fail if the set was immutable or cause concurrency issues if read elsewhere. The fix uses `Sets.difference(components, Collections.singleton(Component.DATA))` to create a read-only view without modifying the original collection.

## Example
```java
// BEFORE: mutates live set, then passes it
components.remove(Component.DATA);
SSTable.delete(desc, components);
```
```java
// AFTER: passes a derived difference view, original set unchanged
SSTable.delete(desc, Sets.difference(components, Collections.singleton(Component.DATA)));
```
