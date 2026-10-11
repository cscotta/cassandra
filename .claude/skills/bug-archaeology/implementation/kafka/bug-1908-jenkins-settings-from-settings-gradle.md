**Commit**: 81447c7c95 · **Ticket**: KAFKA-18153
**Subsystem**: settings.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
def isJenkins = System.getenv('JENKINS_URL') != null
def isCI = isGithubActions || isJenkins
        uploadInBackground = !isCI
        if (isJenkins) {
            tag "jenkins"
        } else if (isGithubActions) {
        enabled = !isJenkins

// After
        uploadInBackground = !isGithubActions
        if (isGithubActions) {
        enabled = true
```

**How to spot**
When reviewing settings.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18153 remove jenkins settings from settings.gradle (#18026).

**Test**
No test added.
