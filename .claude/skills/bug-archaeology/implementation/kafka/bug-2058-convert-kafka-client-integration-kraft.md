**Commit**: 42ea29c421 · **Ticket**: KAFKA-17925
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))
  @ValueSource(strings = Array("zk", "kraft"))

// After
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
  @ValueSource(strings = Array("kraft"))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17925 convert kafka client integration tests to use kraft (#17670).

**Test**
Test modified in ListOffsetsIntegrationTest.scala covering the corrected behaviour.
