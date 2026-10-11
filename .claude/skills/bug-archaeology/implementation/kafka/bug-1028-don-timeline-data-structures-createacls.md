**Commit**: 9dd4cff2d7 · **Ticket**: KAFKA-19347
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
This patch fixes a problem in AclControlManager where we are updating
the timeline data structures prematurely. Reviewers: Alyssa Huang <ahuang@confluent.io>, Colin P. McCabe <cmccabe@apache.org>, Andrew Schofield <aschofield@confluent.io>,

**Before/After**
```java
// Before
            if (existingAcls.add(standardAcl)) {
                idToAcl.put(standardAclWithId.id(), standardAcl);

// After
            if (!existingAcls.contains(standardAcl)) {
            } else {
                log.debug("Not creating ACL since it already exists: {}", standardAcl);
        RecordTestUtils.replayAll(manager, createResult.records());
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19347 don't update timeline data structures in createacls (#19840).

**Test**
Test modified in AclControlManagerTest.java covering the corrected behaviour.
