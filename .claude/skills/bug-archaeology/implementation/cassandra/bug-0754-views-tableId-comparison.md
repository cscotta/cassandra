# Bug 0754: Views.forTable compares TableId wrapper object instead of UUID

**Commit**: 03662bb360 · **Ticket**: n/a
**Subsystem**: Schema / Materialized Views
**Category**: Logic error in condition

---

## Root Cause
`Views.forTable(UUID tableId)` filtered the view collection using `v.baseTableId.equals(tableId)`. `baseTableId` is a `TableId` value-object wrapper around a `UUID`, not a raw `UUID`. Because `TableId.equals(Object)` only returns `true` when compared with another `TableId` instance, comparing it directly against a `UUID` argument always returns `false`. As a result, `forTable` would return an empty iterable regardless of which table ID was queried, silently hiding all materialized views for every table. The fix calls `v.baseTableId.asUUID()` to unwrap before comparing.

### Before / After
```java
// BEFORE — TableId vs UUID: always false, views never found
return Iterables.filter(this, v -> v.baseTableId.equals(tableId));

// AFTER — compare unwrapped UUIDs
return Iterables.filter(this, v -> v.baseTableId.asUUID().equals(tableId));
```

---

## How to Spot
Any time a wrapper/value-object type is introduced around a primitive identifier (UUID, int, String), search every call-site that passes or compares raw primitives against the wrapper and ensure the comparison uses the appropriate accessor. IDE inspections or a type-safe comparator would have caught this immediately.

---

## Test
No test added.
