**Commit**: 60ea0999fb · **Ticket**: KAFKA-17728
**Subsystem**: raft/README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    bin/test-kraft-server-start.sh --config config/kraft.properties
    bin/test-kraft-server-start.sh --config config/kraft-quorum-1.properties
    bin/test-kraft-server-start.sh --config config/kraft-quorum-2.properties
    bin/test-kraft-server-start.sh --config config/kraft-quorum-3.properties

// After
    bin/test-kraft-server-start.sh --config config/kraft.properties --replica-directory-id b8tRS7h4TJ2Vt43Dp85v2A
    bin/test-kraft-server-start.sh --config config/kraft-quorum-1.properties --replica-directory-id b8tRS7h4TJ2Vt43Dp85v2A
    bin/test-kraft-server-start.sh --config config/kraft-quorum-2.properties --replica-directory-id Nkij_D9XRiYKNb41SiJo7Q
    bin/test-kraft-server-start.sh --config config/kraft-quorum-3.properties --replica-directory-id 4-e97nI7eHPYKfEDtW8rtQ
```

**How to spot**
When reviewing raft/README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17728 add missing config `replica-directory-id` to raft readme (#17518).

**Test**
No test added.
