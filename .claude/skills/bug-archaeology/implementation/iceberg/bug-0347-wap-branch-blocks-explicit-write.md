# Bug 0347: Session WAP branch incorrectly blocks writes to explicitly named branch

**Commit**: c7ed71e970781ca6c7fa44a3900625600830e9d4 · **Ticket**: #15288

## Detection Rule

When a write operation targets an explicitly configured branch (via table identifier or write option) AND a session-level WAP branch is also set, verify that the explicit branch takes priority rather than throwing an error.

## Code Smell

A branch-resolution method that throws when both `branch` (from identifier) and `wapBranch` (from session config) are non-null, rather than letting the explicit per-operation branch override the session-scoped default.

## Tags

- scope: project-specific
- category: logic-error
- code-pattern: explicit-overrides-session-default
- severity: crash
- subsystem: spark/branch-resolution

## Root Cause

`determineWriteBranch` and `SparkWriteConf.branch()` treated the session WAP branch as a peer of the explicitly specified branch and raised a `ValidationException` if both were set. The WAP branch is a session-level default that should only activate when no explicit branch is specified; explicit branches (from the table identifier or write options) must always take priority over session defaults.

## Example

```java
// BEFORE — throws ValidationException when session WAP_BRANCH is set and user writes to branch_X
public static String determineWriteBranch(SparkSession spark, String branch) {
    String wapBranch = spark.conf().get(SparkSQLProperties.WAP_BRANCH, null);
    if (wapBranch != null) {
        ValidationException.check(
            branch == null,
            "Cannot write to both branch and WAP branch, but got branch [%s] and WAP branch [%s]",
            branch, wapBranch);
        return wapBranch;
    }
    return branch;
}
```

```java
// AFTER — explicit branch silently wins; WAP branch only used when no branch is specified
public static String determineWriteBranch(
    SparkSession spark, Table table, String branch, CaseInsensitiveStringMap options) {
    String optionBranch = options.get(SparkWriteOptions.BRANCH);
    if (optionBranch != null) {
        Preconditions.checkArgument(branch == null || optionBranch.equals(branch), ...);
        return optionBranch;
    }
    if (branch == null && wapEnabled(table)) {
        return wapSessionBranch(spark);
    }
    return branch;
}
```
