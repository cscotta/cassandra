# Bug 0438: publish_changes silently picks first matching snapshot when WAP ID is duplicated

**Commit**: a55d1235d20a542b7de4cc60772dad172a414861 · **Ticket**: #14955

## Detection Rule

When a procedure or operation selects a unique entity by searching a collection using a "find first" helper, verify that the code validates uniqueness and fails explicitly if more than one match exists, rather than silently applying the first result.

## Code Smell

`Iterables.find(table.snapshots(), predicate, null)` wrapped in `Optional.ofNullable()` — a find-first call that returns as soon as one match is found, silently ignoring any additional matches in the snapshot list.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: find-first-no-uniqueness-check
- severity: silent-wrong-result
- subsystem: spark/procedures

## Root Cause

`PublishChangesProcedure` used `Iterables.find()` to locate the snapshot matching the given WAP ID. When multiple writes shared the same WAP ID (which is user-controlled), the procedure silently cherry-picked whichever snapshot `Iterables.find()` returned first — an arbitrary choice — rather than refusing to act on an ambiguous state. The fix replaces the find-first with an explicit linear scan that throws `ValidationException` as soon as a second match is found.

## Example

```java
// BEFORE
Optional<Snapshot> wapSnapshot = Optional.ofNullable(
    Iterables.find(
        table.snapshots(),
        snapshot -> wapId.equals(WapUtil.stagedWapId(snapshot)),
        null));
if (!wapSnapshot.isPresent()) {
    throw new ValidationException("Cannot apply unknown WAP ID '%s'", wapId);
}
long wapSnapshotId = wapSnapshot.get().snapshotId();
```

```java
// AFTER
Snapshot matchingSnap = null;
for (Snapshot snap : table.snapshots()) {
    if (wapId.equals(WapUtil.stagedWapId(snap))) {
        if (matchingSnap != null) {
            throw new ValidationException(
                "Cannot apply non-unique WAP ID. Found multiple snapshots with WAP ID '%s'", wapId);
        } else {
            matchingSnap = snap;
        }
    }
}
if (matchingSnap == null) {
    throw new ValidationException("Cannot apply unknown WAP ID '%s'", wapId);
}
```
