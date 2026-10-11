**Commit**: 8be216b24f · **Ticket**: KAFKA-18792
**Subsystem**: .github/pull_request_template.md
**Category**: Logic error in condition

**Root cause**
Adds two new workflows to help us enforce some uniform PR structure. The
first workflow runs in an unprivileged context and simply captures the
PR number into a text file and archives it. This is then used by another
workflow that runs in a privileged context using the code in `trunk` to
actually do the validation. The validation is done using a new Python script.

**Before/After**
```java
// Before
// (no removals)

// After
Delete this text and replace it with a detailed description of your change. The
PR title and body will become the squashed commit message.

If you would like to tag individuals, add some commentary, upload images, or
include other supplemental information that should not be part of the eventual
commit message, please use a separate comment.

If applicable, please include a summary of the testing strategy (including
rationale) for the proposed change. Unit and/or integration tests are expected
for any behavior change and system tests should be considered for larger
changes.
# Licensed to the Apache Software Foundation (ASF) under one or more
```

**How to spot**
When reviewing .github/pull_request_template.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18792 add workflow to check pr format (#18985).

**Test**
No test added.
