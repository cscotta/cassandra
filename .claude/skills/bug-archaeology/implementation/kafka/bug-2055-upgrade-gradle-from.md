**Commit**: 4319bfa7b8 · **Ticket**: KAFKA-17974
**Subsystem**: gradle/dependencies.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  gradle: "8.10",
distributionSha256Sum=682b4df7fe5accdca84a4d1ef6a3a6ab096b3efd5edf7de2bd8c758d95a93703
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-all.zip
    if ! curl -s -S --retry 3 -L -o "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "https://raw.githubusercontent.com/gradle/gradle/v8.10.0/gradle/wrapper/gradle-wrapper.jar"; then

// After
  gradle: "8.10.2",
distributionSha256Sum=2ab88d6de2c23e6adae7363ae6e29cbdd2a709e992929b48b6530fd0c7133bd6
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-all.zip
    if ! curl -s -S --retry 3 -L -o "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "https://raw.githubusercontent.com/gradle/gradle/v8.10.2/gradle/wrapper/gradle-wrapper.jar"; then
```

**How to spot**
When reviewing gradle/dependencies.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17974 upgrade gradle from 8.10 to 8.10.2 (#17734).

**Test**
No test added.
