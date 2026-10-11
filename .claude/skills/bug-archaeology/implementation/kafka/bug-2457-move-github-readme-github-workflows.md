**Commit**: cb835d0d6d · **Ticket**: KAFKA-17461
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
According to GitHub rule:

If a repository contains more than one README file, then the file shown is chosen from locations in the following order: the .github directory, then the repository's root directory, and finally the docs directory. the file .github/readme will override root/readme. Hence, we should move .github/readme to ./github/workflows/readme

Reviewers: David Arthur <mumrah@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
// (no additions)
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17461 move the .github/readme.md to .github/workflows/readme.md (#17069).

**Test**
No test added.
