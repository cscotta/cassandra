**Commit**: 5d1c231674 · **Ticket**: KAFKA-19855
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Fixing a small typo in the README.md file. Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
See [Test Retry Gradle Plugin](https://github.com/gradle/test-retry-gradle-plugin) for and [build.yml](.github/workflows/build.yml) more details.

// After
See [Test Retry Gradle Plugin](https://github.com/gradle/test-retry-gradle-plugin) and [build.yml](.github/workflows/build.yml) for more details.
```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19855 fix a typo in readme.md file (#20809).

**Test**
No test added.
