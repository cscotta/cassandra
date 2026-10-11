# Bug 0070: TxnNamedRead Discards Paging State When Start Key Is MinTokenKey

**Commit**: 9da41308d3 · **Ticket**: CASSANDRA-20261
**Subsystem**: `service/accord/txn/TxnNamedRead`
**Category**: Logic error in condition

---

## Root Cause

`TxnNamedRead.commandForSubrange` determines whether a sub-range read is a *continuation* (i.e., it carries forward a paging state from a previous page) by comparing the read command's left partition position against the Accord range's start key. The comparison was performed via `startTokenKey.equals(r.start())`, where `startTokenKey` was a `TokenKey`. When the range's start happened to be a `MinTokenKey` — a distinct subtype representing the left-inclusive token boundary used in paging continuations — `equals` returned `false` even though the tokens were identical, causing the paging state to be discarded. The fix compares tokens directly (`startPP.getToken().equals(subRangeStartToken)`), which is sufficient because multiple queries in the same batch cannot share the same left token bound.

### Before / After

```java
// BEFORE – fails when r.start() is a MinTokenKey
TokenKey startTokenKey = new TokenKey(command.metadata().id, startPP.getToken());
// ...
return command.withTransactionalSettings(nowInSeconds, subRange,
    startTokenKey.equals(r.start()),   // false for MinTokenKey even at same token
    readsWithoutReconciliation(consistencyLevel));
```

```java
// AFTER – compares tokens directly
boolean isRangeContinuation = startPP.getToken().equals(subRangeStartToken);
return command.withTransactionalSettings(nowInSeconds, subRange,
    isRangeContinuation,
    readsWithoutReconciliation(consistencyLevel));
```

---

## How to Spot

When determining equality of routing keys that can be represented by multiple subtypes (`TokenKey`, `MinTokenKey`, `SentinelKey`), comparing the objects directly via `equals` is unsafe unless all subtypes define cross-type equality. Prefer extracting and comparing the underlying token value whenever the semantic question is about token position rather than object identity. The same pitfall applies to any hierarchy where a sentinel/min/max subtype is used as a boundary marker.

---

## Test

No new test was added specifically for this commit; the fix is covered by the existing `InteropTokenRangeTest` suite (commit 0071 adds paged assertions to that test).
