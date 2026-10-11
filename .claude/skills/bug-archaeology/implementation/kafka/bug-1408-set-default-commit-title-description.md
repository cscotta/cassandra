**Commit**: 7d628401e9 · **Ticket**: KAFKA-18791
**Subsystem**: .asf.yaml
**Category**: Logic error in condition

**Root cause**
Reviewers: Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After

  enabled_merge_buttons:
    squash: true
    squash_commit_message: PR_TITLE_AND_DESC
    merge: false
    rebase: false
```

**How to spot**
When reviewing .asf.yaml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18791 set default commit to pr title and description [2/n] (#18967).

**Test**
No test added.
