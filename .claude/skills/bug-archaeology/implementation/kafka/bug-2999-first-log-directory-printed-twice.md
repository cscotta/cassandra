**Commit**: 0f0c9ecbf3 · **Ticket**: KAFKA-16771
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      case e: Throwable =>
        copier.writeLogDirChanges()
    val availableDir1 = TestUtils.tempDir()
    val availableDir2 = TestUtils.tempDir()
    assertEquals(0, runFormatCommand(stream, Seq(availableDir1.toString, availableDir2.toString)))
    assertTrue(stream.toString().contains("Formatting %s".format(availableDir1)))
    assertTrue(stream.toString().contains("Formatting %s".format(availableDir2)))

// After
      case e: Throwable =>
      copier.writeLogDirChanges()
    val availableDirs = Seq(TestUtils.tempDir(), TestUtils.tempDir(), TestUtils.tempDir()).map(dir => dir.toString)
    assertEquals(0, runFormatCommand(stream, availableDirs))
    val actual = stream.toString().split("\\r?\\n")
    val expect = availableDirs.map("Formatting %s".format(_))
    assertEquals(availableDirs.size, actual.size)
    expect.foreach(dir => {
      assertEquals(1, actual.count(_.startsWith(dir)))
    })

```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16771 first log directory printed twice when formatting storage (#16010).

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
