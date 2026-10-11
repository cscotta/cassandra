**Commit**: 2582d36291 · **Ticket**: KAFKA-18244
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
          commit_sha: ${{ github.event.workflow_run.head_sha }}

// After
          commit_sha: ${{ github.event.pull_request.head.sha }}
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18244: fix empty sha on "pull request labeled" workflow (#18190).

**Test**
No test added.
