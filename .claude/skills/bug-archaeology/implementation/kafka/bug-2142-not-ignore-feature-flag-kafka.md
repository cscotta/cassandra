**Commit**: d7ac865fb0 · **Ticket**: KAFKA-17868
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
    assertTrue(stream.toString().contains("4.0-IV0"),

// After
    Option(namespace.getList[String]("feature")).foreach(
      featureNamesAndLevels(_).foreachEntry {
        (k, v) => formatter.setFeatureLevel(k, v)
      })

  def parseNameAndLevel(input: String): (String, java.lang.Short) = {
    val equalsIndex = input.indexOf("=")
    if (equalsIndex < 0)
      throw new RuntimeException("Can't parse feature=level string " + input + ": equals sign not found.")
    val name = input.substring(0, equalsIndex).trim
    val levelString = input.substring(equalsIndex + 1).trim
    try {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17868: do not ignore --feature flag in kafka-storage.sh (#17597).

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
