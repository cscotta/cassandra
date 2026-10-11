# Bug 0198: RESTTableScan sends wrong useSnapshotSchema flag and drops nested field projections

**Commit**: 0a73da119ff38ee3a98f248b42180caa51001cec · **Ticket**: #15609

## Detection Rule

When a scan class overrides `useRef()` and `useSnapshot()` to implement time-travel semantics, verify that the boolean flag distinguishing "branch scan" from "snapshot/tag scan" is set as an instance field in those overrides and propagated in `newRefinedScan()`, rather than being re-derived from a snapshot ID comparison that cannot distinguish the two cases.

## Code Smell

A boolean computed as `snapshotId != table().currentSnapshot().snapshotId()` to decide whether to send `useSnapshotSchema=true`; this expression is false whenever the scanned snapshot happens to be the current one, even for explicit tag or `useSnapshot()` calls that should set it true.

## Tags

- scope: project-specific
- category: logic-error
- code-pattern: stale-derived-flag
- severity: silent-wrong-result
- subsystem: core/rest/RESTTableScan

## Root Cause

Two bugs existed in `RESTTableScan`. First, `useSnapshotSchema` was computed at plan time by comparing snapshot IDs: `snapshotId != table().currentSnapshot().snapshotId()`. This is incorrect because a tag pointing to the current snapshot would produce `false`, but the server needs `true` to return the snapshot's historical schema. The real distinction is whether the caller used `useSnapshot()` or a tag ref (true) versus a branch ref or no time-travel (false). Second, the column projection only extracted top-level `NestedField` names from the schema, missing nested fields like `address.city`; the fix uses `TypeUtil.getProjectedIds()` and `schema().findColumnName()` to produce fully qualified dotted names matching the pattern used in `SnapshotScan`.

## Example

```java
// BEFORE — wrong flag derivation: misses tags pointing at current snapshot
boolean useSnapShotSchema = snapshotId != table().currentSnapshot().snapshotId();
builder.withSnapshotId(snapshotId).withUseSnapshotSchema(useSnapShotSchema);

// BEFORE — projection drops nested fields
List<String> selectedColumns =
    schema().columns().stream().map(Types.NestedField::name).collect(Collectors.toList());
```

```java
// AFTER — flag set explicitly in useRef/useSnapshot and propagated via newRefinedScan
@Override
public TableScan useRef(String name) {
    SnapshotRef ref = table().refs().get(name);
    this.useSnapshotSchema = ref != null && ref.isTag();
    return super.useRef(name);
}

// AFTER — projection uses fully qualified names
List<Integer> projectedFieldIds = Lists.newArrayList(TypeUtil.getProjectedIds(schema()));
List<String> selectedColumns =
    projectedFieldIds.stream().map(schema()::findColumnName).collect(Collectors.toList());
```
