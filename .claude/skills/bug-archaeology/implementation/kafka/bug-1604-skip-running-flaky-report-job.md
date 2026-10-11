**Commit**: b52150dd0d · **Ticket**: KAFKA-18391
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    if : github.event.repository.fork == 'false'
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18391 skip running "flaky test report" job on forks (#18376).

**Test**
No test added.
