**Commit**: a08db65670 · **Ticket**: KAFKA-16888
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Luke Chen <showuon@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      assertTrue(stream.toString().startsWith("Formatting %s".format(tempDir)))
      assertEquals("All of the log directories are already formatted.%n".format(), stream2.toString())
    assertEquals(availableDirs.size, actual.size)

// After
      assertTrue(stream.toString().split("\\r?\\n").exists(_.startsWith("Formatting %s".format(tempDir))))
      assertEquals(1, stream2.toString().split("\\r?\\n").count(_.startsWith("All of the log directories are already formatted")))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16888 fix failed storagetooltest.testformatsucceedsifalldirectoriesareavailable and storagetooltest.testformatempt.

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
