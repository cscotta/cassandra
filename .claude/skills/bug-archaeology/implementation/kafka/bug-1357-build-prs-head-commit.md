**Commit**: d51817699f · **Ticket**: KAFKA-18500
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
The default checkout behavior for GitHub Actions is to use a special
merge ref which is equivalent to the base branch with the PR merged into
it. While this is crucial for checking compilation issues against trunk,
it significantly diminishes our ability to use any build caching. This patch changes the JUnit test jobs to checkout the HEAD commit of the PR 
when building. The "Compile and Check" step still checks out the merge commit
so we can keep that level of validation.

**Before/After**
```java
// Before
- Inputs can only be strings, no support for typed parameters. See: https://github.com/actions/runner/issues/2238
      test-catalog-days: ${{ steps.test-catalog.outputs.days }}
      - name: Test Catalog Age
        id: test-catalog
    name: Compile and Check Java

// After
- Inputs can only be strings, no support for typed parameters. See: https://github.com/actions/runner/issues/2238

## Troubleshooting

### Gradle Cache Misses

If your PR is running for longer than you would expect due to cache misses, there are a
few things to check.

First, find the cache that was loaded into your PR build. This is found in the Setup Gradle
output. Look for a line starting with "Restored Gradle User Home from cache key".
For example,
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18500 build prs at head commit (#18449).

**Test**
No test added.
