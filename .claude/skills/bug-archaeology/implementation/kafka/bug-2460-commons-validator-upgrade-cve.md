**Commit**: 59d3d7021a · **Ticket**: KAFKA-17437
**Subsystem**: gradle/dependencies
**Category**: Wrong constant / default value

**Root cause**
`commons-validator` version 1.7 had known CVEs (email-validation regex ReDoS). The dependency was upgraded to 1.9.0 to pick up the security and correctness fixes. No API changes were required for existing Kafka usage.

**Before/After**
```java
// Before
commonsValidator: "1.7",

// After
commonsValidator: "1.9.0",
```

**How to spot**
When scanning `gradle/dependencies.gradle` for outdated libraries, cross-reference each dependency version with the CVE database. Libraries that perform regex-heavy validation (email, URL) are particularly prone to ReDoS vulnerabilities in older releases.

**Test**
No test added.
