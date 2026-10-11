# Bug 0584: toDate Function Registered with Wrong Argument Type

**Commit**: 76c7d02c94 · **Ticket**: CASSANDRA-14502
**Subsystem**: `src/java/org/apache/cassandra/cql3/functions/TimeFcts.java`
**Category**: Wrong constant / default value

---

## Root Cause

The `toDate` function was registered in the function list with `SimpleDateType.instance` as its argument type instead of `TimestampType.instance`. Because `toDate` already had a separate registration for `SimpleDateType` (a no-op conversion from date to date), the timestamp-to-date overload was simply missing — the wrong type was passed as the variant to support. Any CQL call to `toDate(timestamp_col)` would therefore fail to resolve.

### Before / After

```java
// BEFORE
toDate(SimpleDateType.instance),   // duplicate — should be TimestampType
toUnixTimestamp(SimpleDateType.instance),
```

```java
// AFTER
toDate(TimestampType.instance),    // correct overload for timestamp -> date
toUnixTimestamp(SimpleDateType.instance),
```

---

## How to Spot

When a family of conversion functions is registered together in a static list, each entry should be reviewed to confirm the argument type passed actually matches the intended semantic of that row. A copy-paste where `SimpleDateType.instance` appears twice in a sequence that should cover `TimestampType` is a strong signal that a variant was accidentally duplicated.

---

## Test

No test added.
