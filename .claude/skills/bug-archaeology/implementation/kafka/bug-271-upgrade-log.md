**Commit**: 2759ea0dfa · **Ticket**: KAFKA-20369
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Upgrade log4j2 from 2.25.3 to 2.25.4 This change updates  all four
artifacts (log4j-api, log4j-core, log4j-1.2-api, log4j-slf4j-impl)

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
- log4j-api-2.25.3
- log4j-core-2.25.3
- log4j-slf4j-impl-2.25.3
- log4j-1.2-api-2.25.3
  log4j2: "2.25.3",

// After
- log4j-api-2.25.4
- log4j-core-2.25.4
- log4j-slf4j-impl-2.25.4
- log4j-1.2-api-2.25.4
  log4j2: "2.25.4",
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20369 upgrade log4j2 to 2.25.4 (#21896).

**Test**
No test added.
