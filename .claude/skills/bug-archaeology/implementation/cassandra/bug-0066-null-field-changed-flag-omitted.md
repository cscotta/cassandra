# Bug 0066: Null Field Accounting Omits Changed Flag When Field Is Null

**Commit**: 1aaf0bb336 · **Ticket**: CASSANDRA-20297
**Subsystem**: `service/accord/AccordJournal`
**Category**: Wrong serialization / deserialization

---

## Root Cause

When the `AccordJournal` command builder replayed diffs in reverse order, encountering a field whose stored value was `null` called `setFieldIsNull(field, flags)` but did *not* also call `setChanged(field, flags)`. The changed-bit for that field was therefore never set, so a later (older) entry for the same field was *not* skipped as it should have been. This caused the builder to overwrite a correctly recovered null with an old non-null value from an earlier journal record, corrupting the reconstructed command state. The fix replaced `setFieldIsNull` with `setFieldIsNullAndChanged`, which atomically marks the field as both null and already-processed.

### Before / After

```java
// BEFORE – sets null flag but not changed flag
if (isNull(field, flags))
{
    this.flags = setFieldIsNull(field, this.flags);
}
else
{
    this.flags = setChanged(field, this.flags);
    deserialize(field, in, userVersion);
}
```

```java
// AFTER – sets both null and changed atomically
if (isNull(field, readFlags))
{
    this.flags = setFieldIsNullAndChanged(field, this.flags);
}
else
{
    this.flags = setChanged(field, this.flags);
    deserialize(field, in, userVersion);
}
```

---

## How to Spot

In any incremental/differential serialization scheme that replays records in reverse, "we have already seen this field" must be tracked with a single unified predicate. If the seen-check (`isChanged`) and the null-sentinel-check (`isNull`) are updated by separate code paths, it is easy to forget to set the changed-bit in the null branch, allowing older records to overwrite a legitimately null value. Audit every branch that handles a null-valued field to ensure it also sets the changed/visited flag.

---

## Test

`AccordJournalBurnTest` un-ignored (previously `@Ignore`) so the burn test now validates round-trip journal replay including null-field handling.
