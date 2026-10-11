# Bug 0083: truncatedApply Deserialization Incorrectly Preserves Result for Non-Outcome Status

**Commit**: 1734011032 · **Ticket**: n/a
**Subsystem**: service/accord/SavedCommand, service/accord/serializers/WaitingOnSerializer
**Category**: Wrong serialization / deserialization

---

## Root Cause

During deserialization of a `TruncatedApply` command, the `result` field was passed to `Command.Truncated.truncatedApply` regardless of whether the status was `TruncatedApplyWithOutcome`. For statuses like `TruncatedApply` or `TruncatedApplyWithDeps`, the `result` should be `null` because the outcome was not preserved; carrying over a non-null result from a previously deserialized value caused incorrect command state to be reconstructed. Additionally, `WaitingOnSerializer.serialize/serializedSize` did not receive the `TxnId` needed to validate the `appliedOrInvalidated` field, allowing key-vs-range domain mismatches to go undetected at the serialization boundary.

### Before / After

```java
// BEFORE – result carried through for all TruncatedApply variants
case TruncatedApply:
    if (attrs.txnId().kind().awaitsOnlyDeps())
        return Command.Truncated.truncatedApply(attrs, status, executeAt, writes, result, executesAtLeast);
    return Command.Truncated.truncatedApply(attrs, status, executeAt, writes, result, null);
```

```java
// AFTER – result nulled out unless status is TruncatedApplyWithOutcome
if (status != TruncatedApplyWithOutcome)
    result = null;
if (attrs.txnId().kind().awaitsOnlyDeps())
    return Command.Truncated.truncatedApply(attrs, status, executeAt, writes, result, executesAtLeast);
return Command.Truncated.truncatedApply(attrs, status, executeAt, writes, result, null);
```

---

## How to Spot

In enum-switched deserialization blocks, verify that every field is explicitly re-initialized (or cleared) for each case rather than relying on a previously assigned value. When a status encodes whether an optional field is present, the presence check must come before the field is used, not just before it is stored.

---

## Test

`WaitingOnSerializerTest` updated to pass `txnId` to the serializer and verify round-trip equality.
