# Bug 0742: cqlsh COPY crashes formatting dates before 1900 due to strftime platform limitation

**Commit**: f0502aa791 · **Ticket**: CASSANDRA-13185
**Subsystem**: cqlsh / COPY / formatting
**Category**: Missing null / bounds check

---

## Root Cause
`strftime()` in the cqlsh formatting layer delegated directly to Python's `datetime.strftime()`. On many platforms (particularly glibc-based Linux), `strftime` raises a `ValueError` for years before 1900 because the underlying C library does not support them. Cassandra's `timestamp` type can store arbitrary epoch-relative timestamps, so dates in the range year 1 to 1899 are valid data. COPY TO would crash on any such value. The fix wraps the call in a `try/except ValueError` block and falls back to emitting the raw milliseconds-since-epoch integer, which COPY FROM already knows how to parse — so round-trips are preserved.

### Before / After
```python
# BEFORE — crashes for years < 1900 on most platforms
return ret_dt.strftime(time_format)

# AFTER — graceful fallback to epoch millis
try:
    return ret_dt.strftime(time_format)
except ValueError:
    return '%d' % (seconds * 1000.0)
```

---

## How to Spot
Any use of `strftime` on user-supplied datetime values should guard against platform-specific year limits (pre-1900 on most Unix systems; also year 0 / negative years). In review, check that the catch covers both the year < 1900 case and the year > 9999 case, and that the fallback format is one the corresponding importer can parse.

---

## Test
No new test added.
