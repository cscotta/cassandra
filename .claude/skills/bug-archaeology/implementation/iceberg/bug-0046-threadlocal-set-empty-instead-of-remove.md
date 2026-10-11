# Bug 0046: ThreadLocal cleared with set(emptyMap) instead of remove(), leaking state in thread pools

**Commit**: 4a31e519b8782d1c5a98f2ad00edbe278cedc4a6 · **Ticket**: #15284 / #16031

## Detection Rule

When cleaning up a `ThreadLocal` in a `finally` block, verify that `remove()` is called rather than `set(emptyValue)`, because `set()` keeps the entry in the thread's `ThreadLocalMap` and will cause state retention or memory leaks when the thread is reused from a pool.

## Code Smell

A `finally` block that calls `SOME_THREAD_LOCAL.set(ImmutableMap.of())` or `set(Collections.emptyList())` instead of `SOME_THREAD_LOCAL.remove()`.

## Tags

- scope: java
- category: state-not-cleaned
- code-pattern: threadlocal-set-instead-of-remove
- severity: silent-wrong-result
- subsystem: spark/CommitMetadata

## Root Cause

`CommitMetadata.withCommitProperties()` used `COMMIT_PROPERTIES.set(ImmutableMap.of())` in its `finally` block to "clear" the thread-local. Setting an empty map leaves the `ThreadLocal` entry alive in the thread's map; when a pooled thread is reused and the code tests whether properties are non-empty it may see the empty map sentinel rather than the absence of an entry. The idiomatic cleanup is `remove()`, which actually removes the entry from the thread's `ThreadLocalMap`.

## Example

```java
// BEFORE
public static <R, E extends Exception> R withCommitProperties(...) throws E {
    COMMIT_PROPERTIES.set(properties);
    try {
        return supplier.get();
    } catch (Exception e) {
        ExceptionUtil.castAndThrow(e, exClass);
        return null;
    } finally {
        COMMIT_PROPERTIES.set(ImmutableMap.of()); // leaves entry in ThreadLocalMap
    }
}
```

```java
// AFTER
    } finally {
        COMMIT_PROPERTIES.remove(); // properly removes the entry
    }
```
