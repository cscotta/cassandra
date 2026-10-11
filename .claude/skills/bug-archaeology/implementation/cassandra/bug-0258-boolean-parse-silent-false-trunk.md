# Bug 0258: Boolean Property Silently Returns False for Invalid Values (Trunk Backport)

**Commit**: 6e3770bc15 · **Ticket**: CASSANDRA-17878
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/PropertyDefinitions.java
**Category**: Silent skip on non-match

---

## Root Cause

This is the trunk-branch version of the same fix applied in commit 2638051af1 for the 4.0 line. `PropertyDefinitions.getBoolean()` used only a positive-match regex (`PATTERN_POSITIVE`) so any value that was not `1`, `true`, or `yes` — including obvious typos and invalid strings — silently returned `false` rather than raising a `SyntaxException`. The trunk patch also converts `bloomFilterFpChance` in `TableParams.Builder` from `Double` (boxed, nullable) to `double` with a sentinel value of `-1`, eliminating a related null-check inconsistency.

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
    throw new SyntaxException(format("Invalid boolean value %s for '%s'...", value, key));
}
```

---

## How to Spot

When a single positive-match regex is used to produce a boolean result, the negative branch is implicitly "everything else", which makes the validator accept arbitrary garbage. Reviewers should look for regexes or conditions that decide a two-valued property and verify that the negative case is explicitly enumerated, not left as a fallthrough.

---

## Test

`PropertyDefinitionsTest` is rewritten to assert `parseBoolean` correctly handles all valid positive/negative strings and throws `SyntaxException` for near-miss inputs like `"tru"` and `"fals"`.
