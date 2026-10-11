# Bug 1884: Empty Column Names Accepted for Compact Table Clustering Columns

**Commit**: 623ed791f9 · **Ticket**: CASSANDRA-6152
**Subsystem**: src/java/org/apache/cassandra/cql3 (QueryProcessor, UpdateParameters)
**Category**: Missing null / bounds check

---

## Root Cause

Validation of column names in `QueryProcessor.validateColumnNames()` was only called on the column name iterable at the query-processor level, but the `UpdateParameters` factory methods (`makeColumn`, `makeTombstone`, `makeRangeTombstone`, `makeTombstoneForOverwrite`) created `Column` and `RangeTombstone` objects without any per-name validation. For compact storage tables, where column names encode clustering column values, this allowed empty (`ByteBuffer` with zero remaining bytes) values to be written, producing corrupt internal column names. The fix extracted a single `validateColumnName(ByteBuffer)` method and called it inside every `UpdateParameters` factory method, ensuring validation happens at the point of object construction regardless of call path.

### Before / After

```java
// BEFORE — UpdateParameters creates columns without validation
public Column makeColumn(ByteBuffer name, ByteBuffer value) {
    return ttl > 0
         ? new ExpiringColumn(name, value, timestamp, ttl)
         : new Column(name, value, timestamp);
}
```

```java
// AFTER — validation at construction time
public Column makeColumn(ByteBuffer name, ByteBuffer value) throws InvalidRequestException {
    QueryProcessor.validateColumnName(name);
    return ttl > 0
         ? new ExpiringColumn(name, value, timestamp, ttl)
         : new Column(name, value, timestamp);
}
```

---

## How to Spot

When a validation method exists at one layer (query processor) but object construction happens at a lower layer (update parameters), the validation can be bypassed by any code path that skips the upper layer. Whenever a factory method is introduced or extended, verify that all invariants are enforced directly in the factory rather than relying on callers.

---

## Test

No test added.
