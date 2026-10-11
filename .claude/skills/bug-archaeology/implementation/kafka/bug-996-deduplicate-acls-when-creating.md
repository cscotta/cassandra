**Commit**: 70b672b808 · **Ticket**: KAFKA-19347
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
In #19840, we broke de-duplication during ACL creation. This patch fixes
that and adds a test to cover this case. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
                StandardAclWithId standardAclWithId = new StandardAclWithId(newAclId(), standardAcl);
                records.add(new ApiMessageAndVersion(standardAclWithId.toRecord(), (short) 0));
        MockClusterMetadataAuthorizer authorizer = new MockClusterMetadataAuthorizer();
        authorizer.loadSnapshot(manager.idToAcl());

// After
        Set<StandardAcl> aclsToCreate = new HashSet<>(acls.size());
                if (aclsToCreate.add(standardAcl)) {
                    StandardAclWithId standardAclWithId = new StandardAclWithId(newAclId(), standardAcl);
                    records.add(new ApiMessageAndVersion(standardAclWithId.toRecord(), (short) 0));
                } else {
                    log.debug("Ignoring duplicate ACL from request: {}", standardAcl);
                }


    @Test
    public void testCreateDedupe() {
        AclControlManager manager = new AclControlManager.Builder().build();
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19347 deduplicate acls when creating (#19898).

**Test**
Test modified in AclControlManagerTest.java covering the corrected behaviour.
