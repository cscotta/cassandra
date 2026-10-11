# Bug 0312: Timestamp Timezone Offset Format Not Parsed Correctly

**Commit**: 4f5feac0df · **Ticket**: CASSANDRA-17467
**Subsystem**: src/java/org/apache/cassandra/serializers/TimestampSerializer.java
**Category**: Silent skip on non-match

---

## Root Cause

`TimestampSerializer` tries a cartesian product of date/time format strings and timezone offset format strings to parse timestamp literals.  The offset patterns `"X"` and `"XXX"` (ISO 8601 compact offsets such as `-0600` and `-06:00`) were listed without a preceding space, so they only matched when the offset was directly adjacent to the time component without a separator.  When the timestamp string contained a space before the numeric offset (e.g. `"1970-01-01 00:00:00 -0600"`), none of the patterns matched and the parse failed.  The fix adds the space-prefixed variants `" X"` (and keeps `"X"` for the spaceless form) so both forms are tried.

### Before / After

```java
// BEFORE
final String[] offsetFormats = new String[]
{
    " z",
    "X",
    " zzzz",
    "XXX"
};
```

```java
// AFTER
final String[] offsetFormats = new String[]
{
    " z",
    " zzzz",
    " X",
    "X",
    "XXX",
};
```

---

## How to Spot

When a parser tries a set of format patterns in sequence and any non-match silently moves on to the next, check that every plausible input variant—including whitespace-delimited and non-delimited forms of the same token—is covered.  Timestamp offset parsing is especially prone to this because RFC 3339, ISO 8601, and common locale formats differ only in whether a space precedes the numeric offset.

---

## Test

Adds `validateStringTimestamp` assertions for `"1970-01-01 00:00:00 -0600"`, `"1970-01-01 00:00:00-0600"`, `"1970-01-01T00:00:00 -0600"`, and `"1970-01-01T00:00:00-0600"` in `TimestampSerializerTest`, all of which must decode to the CST epoch offset.
