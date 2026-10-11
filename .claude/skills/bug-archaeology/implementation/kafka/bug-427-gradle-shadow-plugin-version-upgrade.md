**Commit**: 3fd60177ac · **Ticket**: KAFKA-19761
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
**Prologue:**
https://github.com/apache/kafka/pull/19513#discussion_r2365708218

:bulb: **Rationale:**   Gradle 10 will be released in Q4 2026 or Q1 2027
and we can't expect that Shadow plugin version 8 will be compatible:

<img width="780" height="215" alt="image"

src="https://github.com/user-attachments/assets/f9db215c-f6c8-4a6f-ad9d-f5fbb2871482"
/>

**Release notes for Shadow plugin:**
- https://gradleup.com/shadow/changes/#931-2026-01-06
___
I did some testing, and results look just fine :white_check_mark::
- gradle builds `./gradlew :jmh-benchmarks:shadowJar` and `./gradlew
releaseTarGz` are successful
- generated distributions (`kafka_2.13-4.3.0-SNAPSHOT-site-docs.tgz and`
and  `kafk...

**Before/After**
```java
// Before
  id 'com.gradleup.shadow' version '8.3.9' apply false

// After
  id 'com.gradleup.shadow' version '9.3.1' apply false
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19761 gradle shadow plugin version upgrade: 8.3.9 -->> 9.3.1 (#20866).

**Test**
No test added.
