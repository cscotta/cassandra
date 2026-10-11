# Bug 2331: NPE during ColumnFamilyStore initialization due to compaction strategy circular reference

**Commit**: 043d180836 · **Ticket**: n/a

## Detection Rule

When a setter method triggers a side effect (like scheduling background work) that depends on object state set later in the constructor, guard the side effect with a null check for that state.

## Code Smell

A setter like `setMinimumCompactionThreshold()` that unconditionally calls `CompactionManager.instance.submitBackground(this)` — if called from inside the `CompactionStrategy` constructor (which is itself called during `ColumnFamilyStore` construction before `compactionStrategy` is assigned), `this.compactionStrategy` is still `null`.

## Tags

- scope: java
- category: missing-null-check
- code-pattern: circular-constructor-dependency
- severity: crash
- subsystem: compaction

## Root Cause

`ColumnFamilyStore.setMinimumCompactionThreshold()` and `setMaximumCompactionThreshold()` both called `CompactionManager.instance.submitBackground(this)` unconditionally. The compaction strategy is initialized in the CFS constructor, and its own constructor calls these setters to configure thresholds. At the time of these calls, `this.compactionStrategy` in the CFS is still null, so `submitBackground` would receive a CFS with a null strategy and NPE. The fix guards both calls with `if (compactionStrategy != null)`.

## Example

```java
// BEFORE
public void setMinimumCompactionThreshold(int minCompactionThreshold) {
    // ...
    this.minCompactionThreshold.set(minCompactionThreshold);
    CompactionManager.instance.submitBackground(this);  // NPE: compactionStrategy is null during construction
}
```

```java
// AFTER
public void setMinimumCompactionThreshold(int minCompactionThreshold) {
    // ...
    this.minCompactionThreshold.set(minCompactionThreshold);
    // this is called as part of CompactionStrategy constructor; avoid circular dependency
    if (compactionStrategy != null)
        CompactionManager.instance.submitBackground(this);
}
```
