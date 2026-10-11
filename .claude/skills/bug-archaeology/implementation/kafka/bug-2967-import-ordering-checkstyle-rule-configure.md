**Commit**: 342e69192f · **Ticket**: KAFKA-10787
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    ./gradlew checkstyleMain checkstyleTest
  id 'com.diffplug.spotless' version '6.14.0' apply false // 6.14.1 and newer require Java 11 at compile time, so we can't upgrade until AK 4.0

// After
    ./gradlew checkstyleMain checkstyleTest spotlessCheck
#### Spotless ####
The import order is a part of static check. please call `spotlessApply` to optimize the imports of Java codes before filing pull request :

    ./gradlew spotlessApply

  //  the minimum required JRE of 6.14.0+ is 11
  //  refer:https://github.com/diffplug/spotless/tree/main/plugin-gradle#requirements
  id 'com.diffplug.spotless' version "6.13.0" apply false
def spotlessApplyModules = ['']


```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 add import ordering checkstyle rule and configure an automatic formatter (#16097).

**Test**
No test added.
