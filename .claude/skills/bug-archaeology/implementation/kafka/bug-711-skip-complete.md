**Commit**: fa2496bb91 · **Ticket**: KAFKA-18748
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Since that 4.0 branch does not include
[KAFKA-18748](https://issues.apache.org/jira/browse/KAFKA-18748), it is
unable to find the related scan reports, but the ci-complete workflow is
still being triggered on the 4.0 branch. Disable this on the 4.0 branch,
as its reports can be safely ignored. See https://github.com/apache/kafka/pull/20616#issuecomment-3370876779. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    if: (github.event.workflow_run.conclusion == 'success' || github.event.workflow_run.conclusion == 'failure')

// After
    if: (github.event.workflow_run.conclusion == 'success' || github.event.workflow_run.conclusion == 'failure') && github.event.workflow_run.head_branch != '4.0'
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: skip ci-complete on 4.0 (#20644).

**Test**
No test added.
