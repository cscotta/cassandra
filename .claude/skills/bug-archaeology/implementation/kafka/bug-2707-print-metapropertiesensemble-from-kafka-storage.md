**Commit**: a66a59f427 · **Ticket**: KAFKA-17148
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
    stream.println(s"metaPropertiesEnsemble=$metaPropertiesEnsemble")
      assertTrue(stringAfterFirstLine(stream.toString()).startsWith("Formatting %s".format(tempDir)))
      assertEquals("All of the log directories are already formatted.%n".format(), stringAfterFirstLine(stream2.toString()))
  def stringAfterFirstLine(input: String): String = {
    val firstNewline = input.indexOf("\n")
    input.substring(firstNewline + 1)
  }

    assertEquals(availableDirs.size + 1, actual.size)

// After
      assertTrue(stream.toString().startsWith("Formatting %s".format(tempDir)))
      assertEquals("All of the log directories are already formatted.%n".format(), stream2.toString())
    assertEquals(availableDirs.size, actual.size)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17148: remove print metapropertiesensemble from kafka-storage tool (#16607).

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
