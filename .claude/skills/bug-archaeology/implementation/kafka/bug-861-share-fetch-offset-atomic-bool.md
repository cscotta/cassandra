**Commit**: f52f2b99e5 · **Ticket**: KAFKA-19476

**Subsystem**: Share Groups / ShareConsumeRequestManager

**Category**: Race condition / missing synchronization

**Root cause**
`findNextFetchOffset` was declared as an `AtomicBoolean` inside `ShareConsumeRequestManager`, but all code paths that read or write it already held the object's intrinsic lock. Using `AtomicBoolean` in a fully synchronized context provides no benefit while obscuring the synchronization model. The fix converts the field to a plain `boolean` and adds explicit `synchronized` accessors, making the locking contract visible and eliminating the false impression that lock-free access was intended.

**Before/After**
```java
// Before: AtomicBoolean under lock (misleading)
private final AtomicBoolean findNextFetchOffset = new AtomicBoolean(false);

// After: plain boolean with synchronized accessors
private boolean findNextFetchOffset = false;

synchronized boolean findNextFetchOffset() { return findNextFetchOffset; }
synchronized void setFindNextFetchOffset(boolean v) { findNextFetchOffset = v; }
```

**How to spot**
When an `AtomicBoolean`/`AtomicInteger` field is only accessed inside `synchronized` blocks or methods, the atomic wrapper is redundant and misleading. Grep for `AtomicBoolean` fields whose every access site is already guarded by a lock.

**Test**
`ShareConsumeRequestManagerTest` exercises concurrent fetch-offset updates and confirms that the plain boolean under lock produces the same correct ordering as the previous atomic variant.
