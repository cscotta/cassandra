# Bug 2216: get_range_slices Ignores end_token When start_key Is Specified

**Commit**: 4d637f1f1b · **Ticket**: CASSANDRA-4804
**Subsystem**: src/java/org/apache/cassandra/thrift/CassandraServer.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The Thrift `get_range_slices` API accepts two mutually exclusive range forms: token-based (`start_token`/`end_token`) and key-based (`start_key`/`end_key`). When `start_key` was provided together with an `end_token` (a mixed-mode request), the code entered the key-based branch and constructed the end bound exclusively from `end_key`, ignoring `end_token` entirely. This caused the range scan to use a null or wrong upper bound instead of the caller-supplied token boundary.

### Before / After

```java
// BEFORE
bounds = new Bounds<RowPosition>(RowPosition.forKey(range.start_key, p),
                                 RowPosition.forKey(range.end_key, p));
```

```java
// AFTER
RowPosition end = range.end_key == null
        ? p.getTokenFactory().fromString(range.end_token).maxKeyBound(p)
        : RowPosition.forKey(range.end_key, p);
bounds = new Bounds<RowPosition>(RowPosition.forKey(range.start_key, p), end);
```

---

## How to Spot

When a method accepts two parallel fields that may overlap in meaning (e.g. `end_key` and `end_token`), check every branch that reads one field to ensure it also handles the case where that field is absent and the other field is the intended source of truth.

---

## Test

No test added.
