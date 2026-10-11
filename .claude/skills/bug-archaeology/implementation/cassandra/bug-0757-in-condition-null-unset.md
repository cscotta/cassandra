# Bug 0757: Unset values in IN conditions silently included in comparisons

**Commit**: 70e33d96e1 · **Ticket**: CASSANDRA-12981
**Subsystem**: CQL / LWT conditions
**Category**: Missing null / bounds check

---

## Root Cause
`ColumnCondition.Bound` builds the set of values to match against in an LWT `IF col IN (...)` condition. For the scalar case, individual `UNSET` marker values (`ByteBufferUtil.UNSET_BYTE_BUFFER`) were added to the candidate list unchanged instead of being silently skipped, meaning an `UNSET` placeholder could match an actual column value. For the list-bound (marker) case, a `null` or `UNSET_VALUE` result from `bind()` was cast directly to `Lists.Value` without checking, causing a `NullPointerException` or `ClassCastException`. For collection-typed columns (List/Map/Set), unset elements within the bound list were also included rather than skipped. The fixes add guards at every point where a bound value is obtained: null/unset list markers throw `InvalidRequestException`, and individual `UNSET_BYTE_BUFFER` elements are skipped via `continue`.

### Before / After
```java
// BEFORE (scalar IN) — unset value included as a real comparand
for (Term value : condition.inValues)
    this.inValues.add(value.bindAndGet(options));

// AFTER — unset elements are skipped
for (Term value : condition.inValues)
{
    ByteBuffer buffer = value.bindAndGet(options);
    if (buffer != ByteBufferUtil.UNSET_BYTE_BUFFER)
        this.inValues.add(value.bindAndGet(options));
}

// BEFORE (list marker) — NPE/CCE on null or unset list
this.inValues = ((Lists.Value) condition.value.bind(options)).getElements();

// AFTER — explicit null/unset guards
Terminal terminal = condition.value.bind(options);
if (terminal == null)
    throw new InvalidRequestException("Invalid null list in IN condition");
if (terminal == Constants.UNSET_VALUE)
    throw new InvalidRequestException("Invalid 'unset' value in condition");
this.inValues = ((Lists.Value) terminal).getElements();
```

---

## How to Spot
Any place that loops over `Term`/`ByteBuffer` values produced by `bind()`/`bindAndGet()` in a conditional context must handle the two special sentinels: Java `null` (SQL NULL) and `ByteBufferUtil.UNSET_BYTE_BUFFER` / `Constants.UNSET_VALUE` (protocol UNSET). The treatment differs: null is often a valid comparand, while UNSET should be silently dropped from IN lists (or rejected when used as the entire list).

---

## Test
Comprehensive new tests in `InsertUpdateIfConditionTest` covering null/unset behaviour in IN conditions for scalar types, UDTs, lists, maps, and sets — both with individual bound markers and with a list-type marker.
