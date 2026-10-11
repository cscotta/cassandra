# Bug 0184: PendingStats.toComposite Missing Total Field Causes OpenDataException

**Commit**: 9affcf169f · **Ticket**: CASSANDRA-19014
**Subsystem**: repair/consistent/admin
**Category**: Off-by-one

---

## Root Cause

`PendingStats.toComposite()` populates a `CompositeDataSupport` map using indices 2–4 for `pending`, `finalized`, and `failed`, but the composite type has five value fields (0=keyspace, 1=table, 2=total, 3=pending, 4=finalized, 5=failed). The `total` field at index 2 was skipped entirely, leaving its slot empty, and the remaining three fields were written one slot too early. The JMX layer throws `OpenDataException` when `CompositeDataSupport` is constructed with a value missing for a declared item.

### Before / After

```java
// BEFORE
values.put(COMPOSITE_NAMES[2], pending.toComposite());
values.put(COMPOSITE_NAMES[3], finalized.toComposite());
values.put(COMPOSITE_NAMES[4], failed.toComposite());
```

```java
// AFTER
values.put(COMPOSITE_NAMES[2], total.toComposite());
values.put(COMPOSITE_NAMES[3], pending.toComposite());
values.put(COMPOSITE_NAMES[4], finalized.toComposite());
values.put(COMPOSITE_NAMES[5], failed.toComposite());
```

---

## How to Spot

When a method populates a map by string key taken from a positional array (`COMPOSITE_NAMES[N]`), verify that the number of `put` calls matches the total number of declared fields, and that each field name corresponds to the correct positional variable. A mismatch in count between `COMPOSITE_NAMES` length and the number of `put` calls is an immediate red flag.

---

## Test

Runs `repair_admin summarize-pending` against a single-node cluster and asserts the command exits successfully and prints the expected table line with zero-byte stats.
