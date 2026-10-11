**Commit**: 391778b8d7 · **Ticket**: KAFKA-16803
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>, Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
  // Updating the shadow plugin version to 8.1.1 causes issue with signing and publishing the shadowed
  // artifacts - see https://github.com/johnrengelman/shadow/issues/901
  id 'com.github.johnrengelman.shadow' version '8.1.0' apply false
            apply plugin: 'com.github.johnrengelman.shadow'
  apply plugin: 'com.github.johnrengelman.shadow'

// After
  id 'io.github.goooler.shadow' version '8.1.7' apply false
            apply plugin: 'io.github.goooler.shadow'
  apply plugin: 'io.github.goooler.shadow'
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16803: change fork, update shadowjavaplugin to 8.1.7 (#16295).

**Test**
No test added.
