# Bug 0851: Counter Cell Conflict Resolution Crashes on Empty Value

**Commit**: ee609411a8 · **Ticket**: CASSANDRA-11726
**Subsystem**: Storage Engine / Counters
**Category**: Missing null / bounds check

---

## Root Cause
`Conflicts.resolveCounter()` assumed both cells would always carry non-empty value buffers, so it jumped straight to decoding them as counter values.  However, the optimisation introduced in CASSANDRA-10657 allows a counter cell's value to be temporarily empty when the column was not queried by the user.  Passing an empty `ByteBuffer` to the counter-value decoding logic caused an incorrect result or exception.  The fix adds guard clauses: if either value is empty, the method resolves in favour of the non-empty cell (or uses timestamp as a tiebreaker when both are empty) without attempting to decode counter internals.

### Before / After
```java
// BEFORE – jumps straight to MERGE without checking for empty buffers
if (!rightLive) return Resolution.RIGHT_WINS;
return Resolution.MERGE;

// AFTER – handles empty values before attempting MERGE
if (!rightLive) return Resolution.RIGHT_WINS;

if (!leftValue.hasRemaining())
    return rightValue.hasRemaining() || leftTimestamp > rightTimestamp
           ? Resolution.LEFT_WINS : Resolution.RIGHT_WINS;
if (!rightValue.hasRemaining())
    return Resolution.RIGHT_WINS;

return Resolution.MERGE;
```

---

## How to Spot
Whenever an optimisation allows a field that is normally populated to be empty in certain read paths, every downstream consumer of that field must handle the empty case.  Counter conflict resolution is a particularly sensitive site because it is called during read repair and digest comparison — both paths that receive partially-projected rows.  Check that any invariant assumed before an optimisation was introduced is still valid after it.

---

## Test
`CountersTest` extended with a test that reads counter columns through a projection that omits the counter value, then performs a digest comparison to trigger conflict resolution.
