# Bug 2233: ConcurrentModificationException in relocateTokens When Removing Duplicate Tokens

**Commit**: 1078e6f514 · **Ticket**: CASSANDRA-4727
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`StorageService.relocateTokens()` iterated over a `HashSet<Token>` using a for-each loop and called `tokens.remove(srcT)` inside the loop body when a local token was found in the source set. Removing an element from a `HashSet` while iterating it with the for-each iterator throws `ConcurrentModificationException`. Although there is no concurrency involved here, the iterator's structural-modification check still fires on the same-thread removal.

The fix replaces the for-each loop with an explicit `Iterator` so that `it.remove()` can be used, which is the only safe way to remove from a collection during iteration.

### Before / After

```java
// BEFORE
for (Token srcT : tokens)
{
    if (localTokens.contains(srcT))
    {
        tokens.remove(srcT);  // throws ConcurrentModificationException
        logger.warn("cannot move {}; source and destination match", srcT);
    }
}
```

```java
// AFTER
Iterator<Token> it = tokens.iterator();
while (it.hasNext())
{
    Token srcT = it.next();
    if (localTokens.contains(srcT))
    {
        it.remove();  // safe removal via iterator
        logger.warn("cannot move {}; source and destination match", srcT);
    }
}
```

---

## How to Spot

Any for-each loop that calls `collection.remove(element)` on the collection being iterated will throw `ConcurrentModificationException` at runtime. During review, flag every `collection.remove()` call inside a for-each over that same collection and require the explicit-iterator pattern instead.

---

## Test

No test added.
