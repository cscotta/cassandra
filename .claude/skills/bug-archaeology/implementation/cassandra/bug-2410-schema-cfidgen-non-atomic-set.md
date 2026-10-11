# Bug 2410: CQL keyspace creation races to set cfIdGen, can assign duplicate CF IDs

**Commit**: 4af2bfee75 · **Ticket**: CASSANDRA-3903

## Detection Rule

When an atomic integer (or any shared counter) is updated with a computed value that is derived from the current state, use a compare-and-set loop instead of a plain `set()` to avoid lost updates under concurrent access.

## Code Smell

`cfIdGen.set(cfIdMap.size() == 0 ? MIN_CF_ID : Math.max(...))` — a plain `set()` on an `AtomicInteger` after reading its current value is not atomic; a concurrent creator can call `set()` with a lower value after a higher value was already written, rolling the counter back and causing duplicate IDs.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: atomic-integer-non-cas-update
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause

`Schema.fixCFMaxId()` read the current `cfIdGen` value and the maximum existing CF ID, then called `cfIdGen.set(max)`. Under concurrent keyspace creation, two threads could each compute a max value and both call `set()`, with the second overwriting the first with a stale or lower value. This could cause the generator to be set back to a value that had already been used, resulting in duplicate CF IDs. The fix uses a CAS loop: compute `nval = max(current, inMap, MIN_CF_ID)` and retry until `compareAndSet(cval, nval)` succeeds.

## Example

```java
// BEFORE — non-atomic read-compute-set
cfIdGen.set(cfIdMap.size() == 0 ? MIN_CF_ID
            : Math.max(Collections.max(cfIdMap.values()) + 1, MIN_CF_ID));
```

```java
// AFTER — CAS loop
int cval, nval;
do {
    cval = cfIdGen.get();
    int inMap = cfIdMap.isEmpty() ? 0 : Collections.max(cfIdMap.values()) + 1;
    nval = Math.max(Math.max(inMap, cval), MIN_CF_ID);
} while (!cfIdGen.compareAndSet(cval, nval));
```
