# Bug 0256: Boolean Property Silently Returns False for Invalid Values

**Commit**: 2638051af1 · **Ticket**: CASSANDRA-17878
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/PropertyDefinitions.java
**Category**: Silent skip on non-match

---

## Root Cause

`PropertyDefinitions.getBoolean()` used a single regex `PATTERN_POSITIVE` that matched `1`, `true`, or `yes`. Any input that did not match — including clearly invalid strings like `"tru"`, `"falsee"`, or `" true"` with whitespace — was silently treated as `false` rather than raising a `SyntaxException`. Users who mistyped a boolean property (e.g. `cdc = 'tru'`) would get the opposite of their intended value with no error.

### Before / After

```java
// BEFORE
private static final Pattern PATTERN_POSITIVE = Pattern.compile("(1|true|yes)");

public Boolean getBoolean(String key, Boolean defaultValue) throws SyntaxException {
    String value = getSimple(key);
    return (value == null) ? defaultValue : PATTERN_POSITIVE.matcher(value.toLowerCase()).matches();
}
```

```java
// AFTER
private static final Pattern POSITIVE_PATTERN = Pattern.compile("(1|true|yes)");
private static final Pattern NEGATIVE_PATTERN = Pattern.compile("(0|false|no)");

public static boolean parseBoolean(String key, String value) throws SyntaxException {
    String lowerCasedValue = value.toLowerCase();
    if (POSITIVE_PATTERN.matcher(lowerCasedValue).matches()) return true;
    else if (NEGATIVE_PATTERN.matcher(lowerCasedValue).matches()) return false;
    throw new SyntaxException("Invalid boolean value " + value + " for '" + key + "'...");
}
```

---

## How to Spot

When a parser or validator covers only the "true" branch of a two-valued domain and falls back to the negation for everything else, it silently accepts garbage input. Review any code that uses a single positive-match regex to decide a boolean: if the input is not explicitly validated against a negative pattern as well, any typo or unrecognised value will produce the wrong result without surfacing an error to the caller.

---

## Test

Tests for `parseBoolean("cdc", "tru")` and `parseBoolean("cdc", "fals")` assert that a `SyntaxException` is thrown for these near-miss invalid inputs.
