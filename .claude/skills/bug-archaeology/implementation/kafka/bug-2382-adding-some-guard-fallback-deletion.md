**Commit**: d95e384146 · **Ticket**: KAFKA-17508
**Subsystem**: storage/src
**Category**: State not cleaned up

**Root cause**
In KAFKA-16424, we added a fallback logic to delete the logs, but the file has no parent. It'd be better we have some guard from it. Signed-off-by: PoAn Yang <payang@apache.org>

Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.junit.jupiter.api.function.Executable
import org.mockito.Mockito.{doReturn, mock}
  @Test
  def testDeleteIfExistsWithGetParentIsNull(): Unit = {
    val log = mock(classOf[FileRecords])
    val lazyOffsetIndex = mock(classOf[LazyIndex[OffsetIndex]])
    val lazyTimeIndex = mock(classOf[LazyIndex[TimeIndex]])
    val transactionIndex = mock(classOf[TransactionIndex])
    val segment = new LogSegment(log, lazyOffsetIndex, lazyTimeIndex, transactionIndex, 0, 10, 100, Time.SYSTEM)

    // Use "Nil: _*" as workaround for jdk 8
    doReturn(true, Nil: _*).when(log).deleteIfExists()
```

**How to spot**
When reviewing storage/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17508: adding some guard for fallback deletion logic (#17154).

**Test**
Test modified in LogSegmentTest.scala covering the corrected behaviour.
