**Commit**: c40cb07984 · **Ticket**: KAFKA-17911
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
            val final_key = key.replace("KAFKA_", "").toLowerCase()
      "KAFKA_VALID___PROPERTY__ALL_CASES" -> "All Cases Value")
    val expected = List("valid.property=Value", "valid-property_all.cases=All Cases Value")

// After
            val final_key = key.replaceFirst("KAFKA_", "").toLowerCase()
      "KAFKA_VALID___PROPERTY__ALL_CASES" -> "All Cases Value",
      "KAFKA_KAFKA_VALID_PROPERTY" -> "Value")
    val expected = List("valid.property=Value", "valid-property_all.cases=All Cases Value", "kafka.valid.property=Value")
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17911: fix handling of env variables in kafkadockerwrapper (#17655).

**Test**
Test modified in KafkaDockerWrapperTest.scala covering the corrected behaviour.
