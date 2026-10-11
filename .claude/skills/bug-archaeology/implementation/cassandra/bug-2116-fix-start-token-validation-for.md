# Bug 2116: Wide row iteration rejects valid wrap-around range when end_token is the minimum token

**Commit**: 3298c2f19e · **Ticket**: CASSANDRA-5168
**Subsystem**: src/java/org/apache/cassandra/thrift/ThriftValidation.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`ThriftValidation.validateRange()` rejected a `KeyRange` when `start_key`'s token sorted after `end_token`. This check was correct for ordinary ranges but broke the special case where `end_token` is the minimum token (i.e., the empty/sentinel token that represents "end of ring"), which is a valid way to express a range that wraps around to the beginning of the token ring. The missing `!stop.isMinimum()` guard caused the validation to throw `InvalidRequestException` for legitimate wrap-around requests.

### Before / After

```java
// BEFORE
if (RowPosition.forKey(range.start_key, p).compareTo(stop) > 0)
    throw new InvalidRequestException("Start key's token sorts after end token");
```

```java
// AFTER
if (RowPosition.forKey(range.start_key, p).compareTo(stop) > 0 && !stop.isMinimum())
    throw new InvalidRequestException("Start key's token sorts after end token");
```

---

## How to Spot

When validating token or key ordering, check whether the "minimum" sentinel token is handled as a special case meaning "unbounded / wrap around." A comparison that doesn't account for the sentinel will incorrectly reject valid ring-wrap range requests.

---

## Test

No test added.
