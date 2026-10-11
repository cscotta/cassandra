**Commit**: e99da2446c · **Ticket**: KAFKA-15853
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before


// After
    implementation project(':storage')
  dependencies {
    implementation project(':clients')

  }
  checkstyle {
    configProperties = checkstyleConfigProperties("import-control-transaction-coordinator.xml")
  }

      <allow pkg="org.apache.kafka.network" />
      <allow pkg="org.apache.kafka.storage.internals.log" />
<!DOCTYPE import-control PUBLIC
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15853: move kafkaconfig.configdef out of core (#16116).

**Test**
Test modified in DynamicConfigTest.scala covering the corrected behaviour.
