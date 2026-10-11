# Bug 0597: Speculative Retry Percentile Parsing Fails on Non-English Locales

**Commit**: f165e72bf1 · **Ticket**: CASSANDRA-14374
**Subsystem**: `org.apache.cassandra.service.reads`
**Category**: Wrong constant / default value

---

## Root Cause

`PercentileSpeculativeRetryPolicy` uses a `DecimalFormat` to serialize a percentile value (e.g., `99.5`) back to a string so it can be round-tripped through `fromString`. The regex used to parse the string expects a literal ASCII dot as the decimal separator. However, `DecimalFormat` constructed without explicit `DecimalFormatSymbols` inherits the JVM's default locale, so on systems where the default locale uses a comma as the decimal separator (e.g., Brazilian Portuguese), the formatter would produce `"99,5p"` instead of `"99.5p"`. The regex then failed to match, and the round-trip `fromString(toString())` would throw or return the wrong value.

### Before / After

```java
// BEFORE
private static final DecimalFormat FORMATTER = new DecimalFormat("#.####");
```

```java
// AFTER – always use English (dot) separator to match the regex
private static final DecimalFormat FORMATTER =
    new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.ENGLISH));
```

---

## How to Spot

Any use of `new DecimalFormat(pattern)` — or `new DecimalFormat(pattern, /* no symbols */)` — in serialization or parsing code is a locale hazard: the decimal separator, grouping separator, and minus sign all vary by locale. The heuristic is to grep for `new DecimalFormat` and verify that an explicit `DecimalFormatSymbols(Locale.ROOT)` or `Locale.ENGLISH` is supplied whenever the output must be machine-parseable.

---

## Test

`SpeculativeRetryParseTest.testToStringRoundTripCommaDecimalSeparatorLocale` sets the JVM default locale to `pt_BR` and verifies that `fromString(toString())` round-trips correctly for all parameterized retry policy values.
