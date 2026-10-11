**Commit**: cd36d64535 · **Ticket**: KAFKA-18051
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
Kafka Principals must contain a colon. We should enforce this in createAcls. Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
        int colonIndex = binding.entry().principal().indexOf(":");
        if (colonIndex == -1) {
            throw new InvalidRequestException("Could not parse principal from `" +
                binding.entry().principal() + "` " + "(no colon is present separating the " +
                "principal type from the principal name)");
        }
    /**
     * Verify that validateNewAcl catches invalid ACLs with principals that do not contain a colon.
     */
    @Test
    public void testValidateAclWithBadPrincipal() {
        assertEquals("Could not parse principal from `invalid` (no colon is present " +
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18051: disallow creating acls with principals that do not contain a colon (#17883).

**Test**
Test modified in AclControlManagerTest.java covering the corrected behaviour.
