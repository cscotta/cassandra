**Commit**: 89aa87c13f · **Ticket**: KAFKA-19809
**Subsystem**: checkstyle/checkstyle.xml
**Category**: Race condition / missing synchronization

**Root cause**
Jira ticket: KAFKA-19809

Related link: https://checkstyle.org/releasenotes.html#Release_12.2.0

:warning:   quick and dirty (aka skeleton) solution

Reviews are welcome! Relates to: https://github.com/checkstyle/checkstyle/issues/16003

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      <property name="max" value="25"/>
      <property name="max" value="52"/>
                + ", minimum supported request version is 3 which requires brokers to be on version 2.5 or above.");
  checkstyle: project.hasProperty('checkstyleVersion') ? checkstyleVersion : "10.20.2",
                    OffsetFetchResponse.groupError(
                    );
              logContext,
              tokenCache,
              tokenSecretKeyString,
              tokenDefaultMaxLifetimeMs,
              tokenDefaultRenewLifetimeMs);

// After
      <property name="max" value="28"/>
      <property name="max" value="55"/>

    <suppress checks="FinalLocalVariable" files="."/>
    <suppress checks="FinalParameters" files="."/>

                    + ", minimum supported request version is 3 which requires brokers to be on version 2.5 or above.");
  checkstyle: project.hasProperty('checkstyleVersion') ? checkstyleVersion : "12.2.0",
                OffsetFetchResponse.groupError(
                );
                logContext,
                tokenCache,
```

**How to spot**
When reviewing checkstyle/checkstyle.xml code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19809 checkstyle version upgrade: 10 -->> 12 (#20726).

**Test**
No test added.
