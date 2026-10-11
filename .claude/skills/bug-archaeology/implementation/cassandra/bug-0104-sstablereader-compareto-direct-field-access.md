# Bug 0104: SSTableReader.compareTo Uses Direct Field Access, Breaking Mockability

**Commit**: b17e4ee88a · **Ticket**: CASSANDRA-20437
**Subsystem**: io/sstable/format, db/compaction
**Category**: Missing override of default method

---

## Root Cause

`SSTableReader.compareTo` accessed the `instanceId` field directly rather than through a method, and the `UNIQUE_IDENTIFIER_FACTORY` used to create instance identifiers was `private`. When `UnifiedCompactionStrategyTest` creates Mockito mocks of `SSTableReader`, Mockito cannot intercept direct field reads, so the `instanceId` field on every mock was `null`, causing `NullPointerException`s or incorrect equality in the `IntervalTree` comparator. The fix promotes `UNIQUE_IDENTIFIER_FACTORY` to `public`, adds an `instanceId()` accessor, and rewrites `compareTo` to call that accessor so mocks can stub a distinct return value.

### Before / After

```java
// BEFORE
private static final TimeUUID.Generator.Factory<UniqueIdentifier> UNIQUE_IDENTIFIER_FACTORY = ...;

@Override
public int compareTo(SSTableReader other)
{
    return instanceId.compareTo(other.instanceId);
}
```

```java
// AFTER
public static final TimeUUID.Generator.Factory<UniqueIdentifier> UNIQUE_IDENTIFIER_FACTORY = ...;

public UniqueIdentifier instanceId()
{
    return instanceId;
}

@Override
public int compareTo(SSTableReader other)
{
    // Use accessor for instanceId for mocks
    return instanceId().compareTo(other.instanceId());
}
```

---

## How to Spot

When a class exposes final fields or private factories that are read directly inside methods used in data-structures (like `Comparator`), mock-based tests cannot stub them. Review any `compareTo` / `Comparator` implementation that dereferences instance fields directly rather than going through an accessor: any test that mocks the class will silently use `null` or a zero-initialized value, producing incorrect ordering or NPEs that only manifest under Mockito-instrumented code paths.

---

## Test

`UnifiedCompactionStrategyTest` was updated to stub `instanceId()` with a real `TimeUUID` and delegate `compareTo` to the real method, exercising SSTable interval-tree construction with mocked readers.
