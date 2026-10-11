# Bug 0762: Assertion fires for overlapping legacy range tombstones

**Commit**: 52bf7acb05 · **Ticket**: CASSANDRA-12203
**Subsystem**: Storage engine / deserialization (UnfilteredDeserializer)
**Category**: Logic error in condition

---

## Root Cause
When the legacy (pre-3.0) range tombstone deserializer encountered a new tombstone whose start bound was at or before the stop bound of the currently-open tombstone, it was supposed to merge or extend the open range. The assertion that guarded entry into that path was inverted: `> 0` (asserting the new start is strictly after the open stop) instead of `<= 0` (asserting it is at or before). Any real overlap — a perfectly legal pattern in old SSTables — therefore triggered an `AssertionError` during reads or compaction on upgraded clusters.

### Before / After
```java
// BEFORE — asserts new start IS strictly after the open stop (wrong direction)
assert metadata.comparator.compare(tombstone.start.bound, first.stop.bound) > 0;
openTombstones.add(tombstone);
return null;

// AFTER — asserts new start is at or before the open stop (correct: it overlaps)
assert metadata.comparator.compare(tombstone.start.bound, first.stop.bound) <= 0;
openTombstones.add(tombstone);
return null;
```

---

## How to Spot
When a comparator result is used in an assertion at a branch that handles the "overlap" case, verify that the inequality sign matches the semantic intent. `> 0` means "strictly after" while `<= 0` means "before or equal"; swapping them produces an assertion that fires exactly when the code path is legitimately reached.

---

## Test
No test added (CHANGES.txt only accompanies the one-line fix).
