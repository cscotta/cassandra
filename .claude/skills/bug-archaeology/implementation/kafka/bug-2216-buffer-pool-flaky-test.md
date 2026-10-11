**Commit**: e864d8f168 · **Ticket**: KAFKA-17661
**Subsystem**: clients/test (BufferPoolTest)
**Category**: Race condition / missing synchronization

**Root cause**
`testBlockTimeout` in `BufferPoolTest` launched background threads that called `delayedDeallocate` on a shared pool while the main thread was blocked waiting for memory. The timing between thread scheduling and the test assertion was non-deterministic, causing intermittent failures in CI. The test validated the right property (blocking allocation respects timeout) but used multi-threaded machinery that introduced unneeded scheduling races.

**Before/After**
```java
// Before – background thread races with allocation
ExecutorService e = Executors.newSingleThreadExecutor();
e.submit(() -> pool.deallocate(buffer, 1));
assertThrows(TimeoutException.class, () -> pool.allocate(2, 1));

// After – single-threaded: fill pool then try to over-allocate
pool.allocate(1, maxBlockTimeMs); // fills the 2-byte pool
assertThrows(TimeoutException.class,
    () -> pool.allocate(2, 200));  // must time out, no room
```

**How to spot**
Tests that combine `ExecutorService` background threads with assertions on shared mutable state are flaky candidates; prefer single-threaded scenarios that exercise the same property deterministically.

**Test**
Test itself is the fix.
