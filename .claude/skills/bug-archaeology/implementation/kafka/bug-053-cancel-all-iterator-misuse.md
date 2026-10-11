**Commit**: d9462c277b · **Ticket**: KAFKA-19857
**Subsystem**: coordinator-common/src/main/java/org/apache/kafka/coordinator/common/runtime/CoordinatorExecutorImpl.java
**Category**: Logic error in condition

**Root cause**
`CoordinatorExecutorImpl.cancelAll()` called `Iterator.remove()` without a preceding call to `Iterator.next()`. The `Iterator` contract requires `next()` to be called before each `remove()`, so calling `remove()` immediately throws `IllegalStateException`. The loop never actually cancelled any tasks; it always exploded on the first iteration whenever there were scheduled tasks.

**Before/After**
```java
// Before: remove() called without next() — always throws IllegalStateException
public void cancelAll() {
    Iterator<String> iterator = tasks.keySet().iterator();
    while (iterator.hasNext()) {
        iterator.remove();   // ERROR: next() never called
    }
}

// After: simply clear the map
public void cancelAll() {
    tasks.clear();
}
```

**How to spot**
Any iterator loop that calls `iterator.remove()` inside a `while (iterator.hasNext())` block without a matching `iterator.next()` call in the same iteration is incorrect. Static analysis and IDE warnings will often flag this. Prefer `Map.clear()` / `Collection.clear()` when the intent is to remove all elements.

**Test**
`testCancelAllTasks` schedules two tasks, calls `cancelAll()`, then verifies the write operations are rejected and the `TaskOperation` callback is never invoked — confirming that after cancellation the task keys are gone.
