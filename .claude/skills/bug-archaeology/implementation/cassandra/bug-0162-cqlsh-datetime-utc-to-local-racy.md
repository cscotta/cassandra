# Bug 0162: datetime_from_utc_to_local Uses Racy Wall-Clock Offset Calculation

**Commit**: bfb5c59342 · **Ticket**: CASSANDRA-18879
**Subsystem**: pylib/cqlshlib/tracing.py
**Category**: Logic error in condition

---

## Root Cause

`datetime_from_utc_to_local` computed the local UTC offset by sampling `time.time()` twice—once for `datetime.fromtimestamp` (local) and once for `datetime.utcfromtimestamp` (UTC)—and subtracting them. This approach is fragile: the offset is sampled at call time rather than at the timestamp being converted, so it can be wrong during DST transitions or on systems where the local timezone changes. The fix replaces the double-sample arithmetic with the standard `datetime.replace(tzinfo=timezone.utc).astimezone()` idiom, which correctly derives the offset from the datetime value itself.

### Before / After

```python
# BEFORE
def datetime_from_utc_to_local(utc_datetime):
    now_timestamp = time.time()
    offset = datetime.fromtimestamp(now_timestamp) - datetime.utcfromtimestamp(now_timestamp)
    return utc_datetime + offset
```

```python
# AFTER
def datetime_from_utc_to_local(utc_datetime):
    return utc_datetime.replace(tzinfo=timezone.utc).astimezone()
```

---

## How to Spot

Any helper that derives a timezone offset from "the current time" rather than from the timestamp being converted will silently produce wrong results around DST boundaries. During review, look for patterns that call `time.time()` or `datetime.now()` purely to extract an offset and then apply that offset to a different datetime value; the correct pattern is to attach timezone information directly to the source datetime and let Python's `astimezone()` handle the conversion.

---

## Test

No test added.
