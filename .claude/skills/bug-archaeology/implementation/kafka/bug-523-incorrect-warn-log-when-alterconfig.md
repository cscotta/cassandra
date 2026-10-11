**Commit**: edd35a693c · **Ticket**: KAFKA-20017
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
Fix incorrect warn log when alterConfig succeeds and add debug logging
for successful updates. Reviewers: Luke Chen <showuon@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
                    } else {

// After
                    } else if (e != null) {
                    } else {
                        log.debug("Successfully altered configuration of topic {}.", k.name());
```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20017 incorrect warn log when alterconfig suceeds (#21208).

**Test**
No test added.
