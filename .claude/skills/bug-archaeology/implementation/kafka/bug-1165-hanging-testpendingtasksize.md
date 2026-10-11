**Commit**: a04c2fed04 · **Ticket**: KAFKA-19180
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The check for `scheduler.pendingTaskSize()` may fail if the thread pool
is too slow to consume the runnable objects

Reviewers: Ken Huang <s7133700@gmail.com>, PoAn Yang
 <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    assertEquals(2, scheduler.pendingTaskSize())
    assertEquals(0, scheduler.pendingTaskSize())

// After
    retry(30000) {
      assertEquals(2, scheduler.pendingTaskSize())
    }
    retry(30000) {
      assertEquals(0, scheduler.pendingTaskSize())
    }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19180 fix the hanging testpendingtasksize (#19526).

**Test**
Test modified in SchedulerTest.scala covering the corrected behaviour.
