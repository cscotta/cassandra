# Bug 1275: AssertionError in CompressionParameters When resetlocalschema Runs During Repair

**Commit**: a85e731f29 · **Ticket**: CASSANDRA-9249
**Subsystem**: io/compress/CompressionParameters.java
**Category**: Logic error in condition

---

## Root Cause

`CompressionParameters.setLiveMetadata` contained an assertion that the `liveMetadata` field was either null or already pointed to the exact same object being set. The intent was to guard against accidental reassignment, but during repair the schema can be reset (`resetlocalschema`) and then re-applied, legitimately producing a new `CFMetaData` instance for the same table. When that new instance called `setLiveMetadata`, the assertion fired because the old non-null reference differed from the new one by object identity even though they represented the same logical table. The assertion was overly strict and did not account for schema refresh scenarios.

### Before / After

```java
// BEFORE
public void setLiveMetadata(CFMetaData liveMetadata)
{
    if (liveMetadata == null)
        return;

    assert this.liveMetadata == null || this.liveMetadata == liveMetadata;
    this.liveMetadata = liveMetadata;
}
```

```java
// AFTER
public void setLiveMetadata(CFMetaData liveMetadata)
{
    if (liveMetadata == null)
        return;

    this.liveMetadata = liveMetadata;
}
```

---

## How to Spot

Assertions that enforce object-identity equality (`==`) on cached metadata references are fragile in systems where schema objects are replaced rather than mutated in-place. When the system has an operation that rebuilds schema from scratch (like `resetlocalschema`), any cached reference to an old schema object will fail identity checks. Prefer structural equality or simply allow the reassignment, since the field is a non-owning reference to authoritative metadata held elsewhere.

---

## Test

No test added.
