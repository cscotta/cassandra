# Bug 0504: SASI TrieMemIndex Empty Check Race with ConcurrentSkipListSet

**Commit**: 731dfda992 · **Ticket**: CASSANDRA-15526
**Subsystem**: `src/java/org/apache/cassandra/index/sasi/memory`
**Category**: Race condition / missing synchronization

---

## Root Cause

`ConcurrentSkipListMap` (and the `ConcurrentSkipListSet` built on top of it) does not guarantee that `isEmpty()` and `size()` are consistent with each other or with structural operations such as `first()` / `last()`. In particular, a set can appear non-empty to `isEmpty()` while a concurrent removal leaves it actually empty by the time `first()` or `last()` is called, which would throw `NoSuchElementException`. The same inconsistency means the `size()` value captured after `isEmpty()` might not reflect the true count at iteration time. The fix captures the result of a single `size()` call and uses it for both the emptiness test and the count passed to `KeyRangeIterator`, eliminating the TOCTOU gap.

### Before / After

```java
// BEFORE (TrieMemIndex)
if (!keys.isEmpty())
    builder.add(new KeyRangeIterator(keys)); // size() called again internally

// BEFORE (KeyRangeIterator constructor)
public KeyRangeIterator(ConcurrentSkipListSet<DecoratedKey> keys) {
    super(..., keys.size()); // separate size() call, stale vs isEmpty()
```

```java
// AFTER
int size;
if ((size = keys.size()) > 0)
    builder.add(new KeyRangeIterator(keys, size)); // single size snapshot

// AFTER (KeyRangeIterator constructor)
public KeyRangeIterator(ConcurrentSkipListSet<DecoratedKey> keys, int size) {
    super(..., size); // uses pre-captured size
```

---

## How to Spot

When guarding against an empty concurrent collection before constructing an iterator over it, watch for two-step checks (`isEmpty()` then `size()`, or `isEmpty()` then `first()`/`last()`) — each call is a fresh snapshot of volatile state. The safe pattern is to capture `size()` once into a local variable and use that single value for both the guard and any downstream consumer that needs the count.

---

## Test

No dedicated test added; the fix is a structural guard change validated by existing index query tests.
