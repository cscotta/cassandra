# Bug 0042: SparkTable equality ignores snapshotId and branch, causing false cache hits

**Commit**: 2e153ca04f2ed6cfbb329e6b9af055fa6e27bb73 · **Ticket**: #15840

## Detection Rule

When an object wraps an underlying resource plus a qualifier (snapshot ID, branch, partition key) and overrides `equals`/`hashCode`, verify that all qualifiers are included in both methods, not just the primary identifier.

## Code Smell

`equals()` and `hashCode()` that compare only `icebergTable.name()` while the class also holds `snapshotId` and `branch` fields; two instances with the same table name but different branches or snapshots are considered equal.

## Tags

- scope: java
- category: logic-error
- code-pattern: incomplete-equality-key
- severity: silent-wrong-result
- subsystem: spark/source/SparkTable

## Root Cause

`SparkTable.equals()` was intentionally using only the table name "to correctly invalidate Spark cache", but this made logically distinct table references (same table, different snapshot or branch) appear identical. Additionally, the WAP-branch delete path mutated the `branch` field directly instead of using a local variable, causing the `branch` used in the equality contract to be silently overwritten mid-operation.

## Example

```java
// BEFORE
@Override
public boolean equals(Object other) {
    SparkTable that = (SparkTable) other;
    // use only name in order to correctly invalidate Spark cache
    return icebergTable.name().equals(that.icebergTable.name());
}

@Override
public int hashCode() {
    // use only name in order to correctly invalidate Spark cache
    return icebergTable.name().hashCode();
}

// WAP delete path mutates instance field:
if (SparkTableUtil.wapEnabled(table())) {
    branch = SparkTableUtil.determineWriteBranch(sparkSession(), branch); // side-effect!
}
```

```java
// AFTER
@Override
public boolean equals(Object other) {
    SparkTable that = (SparkTable) other;
    return icebergTable.name().equals(that.icebergTable.name())
        && Objects.equals(table().uuid(), that.table().uuid())
        && Objects.equals(snapshotId, that.snapshotId)
        && Objects.equals(branch, that.branch);
}

@Override
public int hashCode() {
    return Objects.hash(icebergTable.name(), table().uuid(), snapshotId, branch);
}

// WAP delete path uses local variable:
String writeBranch = branch;
if (SparkTableUtil.wapEnabled(table())) {
    writeBranch = SparkTableUtil.determineWriteBranch(sparkSession(), branch);
}
```
