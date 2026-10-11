# Bug 1258: DynamicList.remove Nulls Node Value Before Acquiring Lock, Defeating Assert

**Commit**: def6b5fa19 · **Ticket**: n/a
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/util/DynamicList.java
**Category**: Race condition / missing synchronization

---

## Root Cause

In `DynamicList.remove`, the line `node.value = null` was placed before the `try` block — meaning it ran before the write lock was held. A subsequent sanity assertion `assert node.value != null` inside the locked region would then always fire, because the field had already been cleared outside the critical section. More importantly, clearing a shared field without the lock creates a data race: another thread could observe the null value before the structural remove from the skip-list had completed, believing the node was already gone.

### Before / After

```java
// BEFORE
lock.writeLock().lock();
node.value = null;          // outside lock guard — data race + defeats assert
try
{
    assert node.value != null;
    size--;
    ...
```

```java
// AFTER
lock.writeLock().lock();
try
{
    assert node.value != null;
    node.value = null;      // inside lock guard — safe and assert meaningful
    size--;
    ...
```

---

## How to Spot

Whenever a `lock.lock()` call is immediately followed by mutations or assertions before the paired `try` block, those statements run outside the critical section. Review the placement of every statement between `lock()` and `try {` and confirm that only truly setup-free, non-shared operations (if any) belong there. Any write to a shared field or assertion about shared state must sit inside the `try`.

---

## Test

No test added.
