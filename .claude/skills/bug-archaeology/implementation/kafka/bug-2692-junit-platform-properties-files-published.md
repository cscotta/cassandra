**Commit**: bc031c0410 · **Ticket**: KAFKA-17121
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Andy Wilkinson <ankinson@mastodon.org.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
      // The junit-platform.properties file is used for configuring and customizing the behavior of the JUnit platform.
      // It should only apply to Kafka's own JUnit tests, and should not exist in the test JAR.
      // If we include it in the test JAR, it could lead to conflicts with user configurations.
      exclude 'junit-platform.properties'
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17121 junit-platform.properties files in published artifacts pollute the test classpath of consumers (#16624).

**Test**
No test added.
