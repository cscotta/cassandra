# Bug 0155: CQL toJson Produces Wrong Timestamp String for Pre-1582 (Negative) Epoch Values

**Commit**: fba4a85b97 · **Ticket**: CASSANDRA-19566
**Subsystem**: `db/marshal/TimestampType`, `db/marshal/DateType`, `serializers/TimestampSerializer`
**Category**: Wrong serialization / deserialization

---

## Root Cause

`TimestampSerializer` used `SimpleDateFormat` backed by `java.util.Date` for JSON output. `SimpleDateFormat` uses the proleptic Gregorian calendar for all dates, which produces incorrect output for timestamps before the Gregorian calendar reform of 15 October 1582: specifically, negative millisecond epoch values are misformatted because `SimpleDateFormat` does not handle `java.util.Date`'s Julian/Gregorian cutover correctly. The fix replaces `SimpleDateFormat` with a `DateTimeFormatterBuilder` (from `java.time`) and changes call sites to pass `Date.toInstant()` rather than the raw `Date` object, ensuring the ISO-8601 proleptic Gregorian rules in `java.time` are applied uniformly.

### Before / After

```java
// BEFORE — SimpleDateFormat on java.util.Date mis-formats pre-1582 timestamps
private static final FastThreadLocal<SimpleDateFormat> FORMATTER_TO_JSON = new FastThreadLocal<SimpleDateFormat>() {
    protected SimpleDateFormat initialValue() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSX");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }
};
// call site:
return date != null ? TimestampSerializer.getJsonDateFormatter().format(date) : "";
```

```java
// AFTER — DateTimeFormatter on Instant handles the full epoch range correctly
private static final FastThreadLocal<Format> FORMATTER_TO_JSON = new FastThreadLocal<Format>() {
    protected java.text.Format initialValue() {
        return new DateTimeFormatterBuilder()
               .appendPattern("yyyy-MM-dd HH:mm:ss.SSSX")
               .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
               .toFormatter()
               .withZone(ZoneId.of("UTC"))
               .toFormat();
    }
};
// call site:
return date != null ? TimestampSerializer.getJsonDateFormatter().format(date.toInstant()) : "";
```

---

## How to Spot

Any use of `SimpleDateFormat` or `java.util.Date` for producing or consuming timestamp strings in a storage system that persists millisecond epoch values is suspect: `java.util.Date` uses a mixed Julian/Gregorian calendar with a cutover at 15 October 1582, so negative epoch milliseconds (i.e., dates before 1 January 1970 BCE) can be formatted incorrectly. Prefer `java.time.Instant` + `DateTimeFormatter` throughout.

---

## Test

No new test added in this commit; the fix is validated by the CHANGES.txt entry and manual verification of the formatter output.
