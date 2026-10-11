**Commit**: 832c7d646c · **Ticket**: KAFKA-18748
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Always mark the result of "missing build scan" as  successful, not as
failing, for 4.0 and 4.1. Two reasons causing missing build scan:
- 4.0 does not have the KAFKA-18748 patches. - The JDK version used by `ci-complete` is inconsistent with other
active branches, causing the build report task to fail. The real solution will be in
[KAFKA-19768](https://issues.apache.org/jira/browse/KAFKA-19768).

**Before/After**
```java
// Before
    if: (github.event.workflow_run.conclusion == 'success' || github.event.workflow_run.conclusion == 'failure') && github.event.workflow_run.head_branch != '4.0'
          state: 'error'

// After
    if: (github.event.workflow_run.conclusion == 'success' || github.event.workflow_run.conclusion == 'failure')
          state: 'success' # Always mark as successful as a temporary fix; non-trunk branches will miss build scan. Real fix in KAFKA-19768
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: ignore result of "missing build scan" (#20659).

**Test**
No test added.
