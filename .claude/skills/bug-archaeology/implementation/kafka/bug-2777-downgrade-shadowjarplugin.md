**Commit**: a33cc3d351 · **Ticket**: KAFKA-17052
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
This version of the ShadowJarPlugin uses the incorrect classifier for the published archive. This is a temporary measure to fix publishing prior to the upcoming release. Signed-off-by: Greg Harris <greg.harris@aiven.io>


Reviewers:  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
  id 'io.github.goooler.shadow' version '8.1.7' apply false

// After
  id 'io.github.goooler.shadow' version '8.1.3' apply false
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17052: downgrade shadowjarplugin to 8.1.3 (#16489).

**Test**
No test added.
