# Bug 0071: Accord Range Read Paging Breaks When Start and End Token Are Equal

**Commit**: f55653ecb0 · **Ticket**: CASSANDRA-20262
**Subsystem**: `service/accord/txn/TxnNamedRead`
**Category**: Logic error in condition

---

## Root Cause

When building an Accord `AccordRoutingKey` for the left bound of a paged range read, `TxnNamedRead.boundsAsAccordRange` chose between `TokenKey` and `MinTokenKey` based on whether the read was left-inclusive or started with a minimum key bound. It did not account for the case where the start token equals the stop token — a degenerate range produced by paging when a single token straddles the page boundary. In that case the method created a `TokenKey` (exclusive left), which made the range empty and caused the read to return zero rows. The fix adds an `|| startToken.equals(stopToken)` guard that upgrades the boundary to a `MinTokenKey` (inclusive left) so the full single-token range is covered.

### Before / After

```java
// BEFORE – degenerate single-token range yields empty read
if (startToken.isMinimum() && inclusiveLeft)
    startAccordRoutingKey = SentinelKey.min(tableId);
else if (inclusiveLeft || startIsMinKeyBound)
    startAccordRoutingKey = new MinTokenKey(tableId, startToken);
else
    startAccordRoutingKey = new TokenKey(tableId, startToken);  // exclusive – misses equal-token range
```

```java
// AFTER – equal start/stop token forces inclusive left boundary
else if (inclusiveLeft || startIsMinKeyBound || startToken.equals(stopToken))
    startAccordRoutingKey = new MinTokenKey(tableId, startToken);
else
    startAccordRoutingKey = new TokenKey(tableId, startToken);
```

---

## How to Spot

Range construction code that translates partition-position bounds into routing keys must handle the degenerate case where the left and right token are identical. In that situation an exclusive left boundary produces an empty range; the correct behavior is to treat it as left-inclusive so that rows at that exact token are included. When reviewing token-range boundary translation, check all three-way branch conditions (minimum, inclusive/minKeyBound, default) for the missing equal-token case.

---

## Test

`InteropTokenRangeTest` extended with paged queries (`executeWithPagingWithResult`) to assert that all expected tokens are returned even when paging reduces the effective range to a single token.
