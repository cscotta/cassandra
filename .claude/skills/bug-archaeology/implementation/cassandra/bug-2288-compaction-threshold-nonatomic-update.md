# Bug 2288: nodetool setcompactionthreshold Validates Against Intermediate State

**Commit**: cc0be1b400 · **Ticket**: CASSANDRA-4455

## Detection Rule

When setting two related threshold values that must satisfy an ordering invariant, verify that both values are validated together atomically rather than each individually against the current (partially updated) state.

## Code Smell

`setMinimumCompactionThreshold` validates `minThreshold <= maxThreshold` against the *current* `maxCompactionThreshold`, and `setMaximumCompactionThreshold` validates against the *current* `minCompactionThreshold`; calling them in sequence (min first, then max) can fail the intermediate validation even when the final state is valid.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: non-atomic-paired-threshold-update
- severity: crash
- subsystem: compaction

## Root Cause

`nodetool setcompactionthreshold` called `setMinimumCompactionThreshold(min)` followed by `setMaximumCompactionThreshold(max)` as separate JMX calls. If the new min exceeded the old max, the first call threw a validation error even if the new max would have satisfied the constraint. The fix introduces `setCompactionThresholds(min, max)` that validates both values together before updating either, and replaces the two-call sequence with a single atomic JMX call.

## Example

```java
// BEFORE — each setter validates against current (not final) peer value
public void setMinimumCompactionThreshold(int min) {
    if (min > this.maxCompactionThreshold.value() && this.maxCompactionThreshold.value() != 0)
        throw new RuntimeException("...");
    this.minCompactionThreshold.set(min);
}
// NodeProbe: cfsProxy.setMinimumCompactionThreshold(min);
//            cfsProxy.setMaximumCompactionThreshold(max); // may fail
```

```java
// AFTER — atomic validation
public void setCompactionThresholds(int min, int max) {
    validateCompactionThresholds(min, max);  // checks both at once
    minCompactionThreshold.set(min);
    maxCompactionThreshold.set(max);
    if (compactionStrategy != null)
        CompactionManager.instance.submitBackground(this);
}
// NodeProbe: cfsProxy.setCompactionThresholds(min, max);
```
