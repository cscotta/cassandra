## Commit

8fc41f9ca2 — KAFKA-19171: TaskManager passes immutable collections to closeTaskClean

## Ticket

KAFKA-19171

## Subsystem

Kafka Streams / task manager

## Category

Logic error in condition

## Root cause

`TaskManager.handleTasksPendingInitialization()` called `closeTaskClean(Collections.emptySet(), Collections.emptyMap())`. The `closeTaskClean` method adds to the supplied `Set` and `Map` to track tasks that were closed cleanly. Because `Collections.emptySet()` and `Collections.emptyMap()` are immutable, any attempt to add to them threw `UnsupportedOperationException`. The exception was caught higher up and surfaced as a misleading task-initialization failure rather than a clear mutation error.

## Before / After

```java
// Before — immutable collections passed to a method that mutates them
closeTaskClean(Collections.emptySet(), Collections.emptyMap());

// After — mutable collections that the method can populate
closeTaskClean(new HashSet<>(), new HashMap<>());
```

## How to spot

Search for calls to methods whose parameter names include "result", "out", "accumulator", or similar output-oriented names, and check whether the caller passes `Collections.emptyXxx()`. Any method that writes into its collection parameters will throw `UnsupportedOperationException` when given an immutable empty collection.

## Test

`TaskManagerTest` was updated to call `handleTasksPendingInitialization` with tasks that require clean-close and to verify that no `UnsupportedOperationException` is thrown.
