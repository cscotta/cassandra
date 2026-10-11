# Bug 2248: Corrupted Saved Caches Due to Null Future in Load Loop

**Commit**: 2c250c660f · **Ticket**: CASSANDRA-4622
**Subsystem**: src/java/org/apache/cassandra/cache/AutoSavingCache.java
**Category**: Missing null / bounds check

---

## Root Cause

`AutoSavingCache` loads persisted cache entries by submitting deserialization tasks as `Future<Pair<K,V>>` objects and collecting them in a list before resolving them. The `deserialize()` call can return `null` (when the backing SSTable no longer exists), but the null check was applied only after calling `future.get()` — by which point `null` had already been added to the futures list. When the loop later called `future.get()` on a null reference it threw a NullPointerException, corrupting or aborting the cache load.

The fix moves the null guard to the point of collection: if `deserialize()` returns `null`, the entry is skipped immediately and never added to the futures list, so the `get()` loop processes only valid futures.

### Before / After

```java
// BEFORE
futures.add(cacheLoader.deserialize(in, cfs));  // null added to list
count++;
...
Pair<K, V> entry = future.get();  // NPE if future is null
if (entry == null)                 // null check too late
    continue;
put(entry.left, entry.right);
```

```java
// AFTER
Future<Pair<K, V>> entry = cacheLoader.deserialize(in, cfs);
if (entry == null)   // guard at collection time
    continue;
futures.add(entry);
count++;
...
Pair<K, V> entry = future.get();  // always non-null
put(entry.left, entry.right);
```

---

## How to Spot

When a method that returns a reference is called inside a collection operation (e.g., `list.add(method())`), verify that the method cannot return `null` before deferring the null check to a later iteration. The safe pattern is to capture the result in a local variable and null-check before adding.

---

## Test

No test added.
