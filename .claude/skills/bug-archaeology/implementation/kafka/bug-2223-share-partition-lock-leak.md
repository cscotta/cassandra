**Commit**: b98aee328b · **Ticket**: KAFKA-17620
**Subsystem**: coordinator/share (SharePartition)
**Category**: Resource leak

**Root cause**
`SharePartition.acquire()` returned `CompletableFuture<List<AcquiredRecords>>` even though the acquire operation is entirely synchronous. The caller was forced to block on the future, and the lock held during acquisition could be leaked if the caller's future completion chain threw before reaching the finally block. Simplifying the return type to `List<AcquiredRecords>` removed the unnecessary async wrapper and allowed the lock release to be moved unconditionally into a `finally` block.

**Before/After**
```java
// Before
CompletableFuture<List<AcquiredRecords>> acquire(String memberId, ...) {
    lock.lock();
    try { ... }
    catch (...) { lock.unlock(); throw; }
    // no finally – lock leaked on unexpected exception
}

// After
List<AcquiredRecords> acquire(String memberId, ...) {
    lock.lock();
    try { return doAcquire(...); }
    finally { lock.unlock(); }
}
```

**How to spot**
When a method wraps a synchronous result in `CompletableFuture.completedFuture(...)`, consider whether the future abstraction adds value; if not, return the value directly and ensure lock release is in `finally`.

**Test**
Existing tests updated to reflect synchronous return type.
