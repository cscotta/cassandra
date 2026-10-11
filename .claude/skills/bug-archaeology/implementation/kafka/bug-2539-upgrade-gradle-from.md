**Commit**: d0cd3a899a · **Ticket**: KAFKA-17180
**Subsystem**: gradle/dependencies.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  gradle: "8.8",
distributionSha256Sum=f8b4f4772d302c8ff580bc40d0f56e715de69b163546944f787c87abf209c961
distributionUrl=https\://services.gradle.org/distributions/gradle-8.8-all.zip
    if ! curl -s -S --retry 3 -L -o "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "https://raw.githubusercontent.com/gradle/gradle/v8.8.0/gradle/wrapper/gradle-wrapper.jar"; then

// After
  gradle: "8.10",
distributionSha256Sum=682b4df7fe5accdca84a4d1ef6a3a6ab096b3efd5edf7de2bd8c758d95a93703
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-all.zip
    if ! curl -s -S --retry 3 -L -o "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "https://raw.githubusercontent.com/gradle/gradle/v8.10.0/gradle/wrapper/gradle-wrapper.jar"; then
```

**How to spot**
When reviewing gradle/dependencies.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17180 upgrade gradle from 8.8 to 8.10 (#16908).

**Test**
No test added.
