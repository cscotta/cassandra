**Commit**: bac8dce3c1 · **Ticket**: KAFKA-20146
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Add a `pip install --dry-run ./tests` step in the `validate` job to
catch dependency conflicts in `tests/setup.py` early, before Gradle
compilation. This addresses the issue from #21415 where dependabot bumped `requests`
to 2.32.4, conflicting with `ducktape==0.12.0`'s pin on
`requests==2.31.0`, which went undetected until it broke kafkatest
installation. Verified locally that `--dry-run` correctly fails with exit code 1 when
a conflict is present. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
      - name: Check Python dependency conflicts
        shell: bash
        run: pip install --dry-run ./tests
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20146 add ci check to detect python dependency conflicts (#21472).

**Test**
No test added.
