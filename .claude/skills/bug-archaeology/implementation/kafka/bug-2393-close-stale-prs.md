**Commit**: ad19d29930 · **Ticket**: KAFKA-15073
**Subsystem**: .github/workflows
**Category**: Resource leak

**Root cause**
As a follow-up of #13827, this patch updates the stale PR workflow to automatically close PRs that have not had activity in 120 days

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Josep Prat <josep.prat@aiven.io>

**Before/After**
```java
// Before
          days-before-close: -1
          stale-pr-message: >
            would like to keep this PR alive, please ask a committer for review. If the PR has
            merge conflicts, please update it with the latest from trunk (or appropriate release branch)


// After
          ascending: true
          days-before-close: 30  # Since adding 'stale' will update the PR, days-before-close is relative to that.
          stale-pr-message: |
            would like to keep this PR alive, please leave a comment asking for a review. If the PR has
            merge conflicts, update it with the latest from the base branch.
            <p>
            If you are having difficulty finding a reviewer, please reach out on the
            [mailing list](https://kafka.apache.org/contact).
          close-pr-label: 'closed-stale'
          close-pr-message: |
            This PR has been closed since it has not had any activity in 120 days. If you feel like this
            was a mistake, or you would like to continue working on it, please feel free to re-open the
```

**How to spot**
When reviewing .github/workflows code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15073 close stale prs [2/n] (#17166).

**Test**
No test added.
