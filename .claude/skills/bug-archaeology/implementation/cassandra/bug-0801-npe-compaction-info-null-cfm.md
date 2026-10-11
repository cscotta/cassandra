# Bug 0801: NPE in CompactionInfo When CFMetaData Is Null

**Commit**: cbebb29adf · **Ticket**: CASSANDRA-12462
**Subsystem**: Compaction
**Category**: Missing null / bounds check

---

## Root Cause
`CompactionInfo` allows `cfm` (the `CFMetaData` field) to be `null` to represent compaction tasks that are not tied to a specific column family (e.g., index summary redistribution or key cache save). The three accessor methods `getId()`, `getKeyspace()`, and `getColumnFamily()` dereferenced `cfm` unconditionally, throwing a `NullPointerException` whenever `compactionstats` was queried while such a task was active.

### Before / After
```java
// BEFORE
public UUID getId()
{
    return cfm.cfId;
}

public String getKeyspace()
{
    return cfm.ksName;
}

public String getColumnFamily()
{
    return cfm.cfName;
}

// AFTER
public UUID getId()
{
    return cfm != null ? cfm.cfId : null;
}

public String getKeyspace()
{
    return cfm != null ? cfm.ksName : null;
}

public String getColumnFamily()
{
    return cfm != null ? cfm.cfName : null;
}
```

---

## How to Spot
Any time a class field is documented or visibly permitted to be `null` (e.g., passed as `null` at construction call-sites or accepted by a nullable constructor parameter), every access to that field must be guarded. Review all public accessor methods of `CompactionInfo` and similar info/stats DTOs during code review to confirm that nullable fields receive null guards before dereference, especially in paths called from monitoring or JMX interfaces.

---

## Test
No test added.
