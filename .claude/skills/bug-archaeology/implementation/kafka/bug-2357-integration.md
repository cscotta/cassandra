**Commit**: 8569cf102b · **Ticket**: KAFKA-17356
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  private val dirLocks = lockLogDirs(liveLogDirs)

// After
  // visible for testing
  private[log] val dirLocks = lockLogDirs(liveLogDirs)
import java.nio.file.attribute.PosixFilePermission

  /**
   * This test simulates an offline log directory by removing write permissions from the directory.
   * It verifies that the LogManager continues to operate without failure in this scenario.
   * For more details, refer to KAFKA-17356.
   */
  @Test
  def testInvalidLogDirNotFailLogManager(): Unit = {
    logManager.shutdown()
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17356 add integration test for kafka-17310 (#17211).

**Test**
Test modified in LogManagerTest.scala covering the corrected behaviour.
