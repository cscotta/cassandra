# Bug 0045: canDeleteWhere scans wrong branch when WAP is active

**Commit**: 5f80bc3a38d582fe352969a9b2ae037de048c843 · **Ticket**: #15512

## Detection Rule

When a delete or write operation resolves a target branch for committing, verify that the read/scan path used to evaluate whether the operation is possible also uses that same resolved branch, not a stale or default branch.

## Code Smell

A `canDelete` or eligibility-check method that builds a table scan using `table().currentSnapshot()` or a hardcoded `snapshot` field instead of calling the same branch-resolution helper used by the actual commit path.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: read-write-branch-mismatch
- severity: silent-wrong-result
- subsystem: spark/source/SparkTable (v4.1)

## Root Cause

`canDeleteWhere` in `SparkTable` determined whether a metadata-only delete was possible by scanning the table, but it always scanned the default snapshot without resolving the WAP branch. The actual delete committed to the WAP branch. When WAP is active the WAP branch has data the main branch does not, so the scan could return a result inconsistent with what the delete would actually affect, leading to wrong rows being targeted or wrong rows surviving.

## Example

```java
// BEFORE
public boolean canDeleteWhere(Predicate[] predicates) {
    // ...resolves deleteExpr but never determines branch...
    return canDeleteUsingMetadata(deleteExpr);
}

private boolean canDeleteUsingMetadata(Expression deleteExpr) {
    // scan always uses current snapshot (main branch)
    if (snapshot != null) {
        scan = scan.useSnapshot(snapshot.snapshotId());
    }
    // ...
}
```

```java
// AFTER
public boolean canDeleteWhere(Predicate[] predicates) {
    // ...
    String scanBranch = SparkTableUtil.determineReadBranch(
        spark(), table(), branch, CaseInsensitiveStringMap.empty());
    return canDeleteUsingMetadata(deleteExpr, scanBranch);
}

private boolean canDeleteUsingMetadata(Expression deleteExpr, String scanBranch) {
    if (scanBranch != null) {
        scan = scan.useRef(scanBranch);
    } else if (snapshot != null) {
        scan = scan.useSnapshot(snapshot.snapshotId());
    }
    // ...
}
```
