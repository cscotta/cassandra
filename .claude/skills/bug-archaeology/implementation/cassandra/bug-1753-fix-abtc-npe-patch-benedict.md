# Bug 1753: Fix AtomicBTreeColumns NPE

**Commit**: 5223c4797c · **Ticket**: CASSANDRA-6692
**Subsystem**: src/java/org/apache/cassandra/db/AtomicBTreeColumns.java, src/java/org/apache/cassandra/utils/btree/BTree.java
**Category**: Missing null / bounds check

---

## Root Cause

In `AtomicBTreeColumns`, the local variable `deletionInfo` was assigned from `cm.deletionInfo()` before the conditional branch. When `mayModify` returned false the variable remained pointing at the incoming (unmerged) deletion info instead of `current.deletionInfo`, so the subsequent `ColumnUpdater` used the wrong deletion context, which could cause a NPE when that uninitialized reference was later dereferenced. Separately, `BTree`'s `ThreadLocal<Builder>` held a single `Builder` instance; recursive calls into `BTree.update` would re-enter the same builder while it was still in use, corrupting its state and producing NPEs. The fix initialises `deletionInfo` only inside the truthy branch and adds an `else` that assigns `current.deletionInfo` for the no-modify path; and changes the `ThreadLocal` to a `Queue<Builder>` so each level of recursion gets its own instance.

### Before / After

```java
// BEFORE (AtomicBTreeColumns)
DeletionInfo deletionInfo = cm.deletionInfo();
if (deletionInfo.mayModify(current.deletionInfo)) {
    ...
    deletionInfo = current.deletionInfo.copy().add(deletionInfo);
}
// deletionInfo may still hold cm's value if branch not taken
```

```java
// AFTER (AtomicBTreeColumns)
DeletionInfo deletionInfo;
if (cm.deletionInfo().mayModify(current.deletionInfo)) {
    ...
    deletionInfo = current.deletionInfo.copy().add(cm.deletionInfo());
} else {
    deletionInfo = current.deletionInfo;
}
```

```java
// BEFORE (BTree)
private static final ThreadLocal<Builder> modifier = new ThreadLocal<Builder>() {
    protected Builder initialValue() { return new Builder(); }
};
// usage: modifier.get().build(...)
```

```java
// AFTER (BTree)
private static final ThreadLocal<Queue<Builder>> modifier = new ThreadLocal<Queue<Builder>>() {
    protected Queue<Builder> initialValue() { return new ArrayDeque<>(); }
};
// usage: poll from queue, use, return to queue
```

---

## How to Spot

Declaring a variable before a conditional that is also supposed to assign it means the else-path silently uses the pre-assigned value. Always verify that every branch through a conditional leaves the variable in a defined, correct state. For `ThreadLocal` objects that hold stateful builders, check for re-entrancy: a recursive call on the same thread will reuse the in-progress builder.

---

## Test

No test added.
