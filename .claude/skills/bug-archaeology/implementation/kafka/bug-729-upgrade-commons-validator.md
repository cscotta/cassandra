**Commit**: 3c0843961b · **Ticket**: KAFKA-19739
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
In [KAFKA-19359](https://issues.apache.org/jira/browse/KAFKA-19359), the
commons-beanutils transitive dependency was force bumped in the project
to avoid related CVEs. The commons-validator already has a new release,
which solves this problem:

https://github.com/apache/commons-validator/tags

The workaround could be deleted as part of the version bump. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
- commons-validator-1.9.0
          // Workaround before `commons-validator` has new release. See KAFKA-19359.
          libs.commonsBeanutils,
  commonsBeanutils: "1.11.0",
  commonsValidator: "1.9.0",
  commonsBeanutils: "commons-beanutils:commons-beanutils:$versions.commonsBeanutils",

// After
- commons-validator-1.10.0
  commonsValidator: "1.10.0",
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19739 upgrade commons-validator to 1.10.0 (#20601).

**Test**
No test added.
