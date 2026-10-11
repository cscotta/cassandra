**Commit**: 68c6c6da86 · **Ticket**: KAFKA-17767
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
This patch adds a CI job to store our test catalog in an orphaned branch named "test-catalog" within this repo. This data will be used to help determine which tests should be quarantined. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After

  update-test-catalog:
    needs: test
    if: always()
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - name: Checkout Test Catalog
        uses: actions/checkout@v4
        with:
          persist-credentials: true  # Needed to commit and push later
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17767 store the test catalog in git [2/n] (2nd attempt) (#17627).

**Test**
No test added.
