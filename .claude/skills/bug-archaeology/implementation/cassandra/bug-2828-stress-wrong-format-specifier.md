# Bug 2828: Stress tool uses float format specifier and inverted tombstone/column logic

**Commit**: 1d8ba59c9b · **Ticket**: CASSANDRA-1915

## Detection Rule
When generating zero-padded integer keys with `String.format`, use `%d` with an explicit `(int)` cast; using `%f` produces floating-point strings that are unrecognized as valid row keys.

## Code Smell
`String.format("%0" + n + "f", token)` formats a numeric token as a floating-point decimal (e.g., `"00000001.000000"`) instead of as a zero-padded integer (e.g., `"00000001"`). The resulting key strings do not match the stored keys, causing all reads to report "Key not found."

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: wrong-format-specifier
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
Two bugs were combined: (1) the key generation format was `%f` (float) instead of `%d` (integer), causing mismatched key names on read; (2) the `null` check on `get_slice` results was wrong — the API never returns `null` for an empty result, it returns an empty list, so `columns == null` never fired whereas `columns.size() == 0` does.

## Example
```java
// BEFORE
String format = "%0" + session.getTotalKeysLength() + "f";
return String.format(format, token).getBytes();

// AFTER
String format = "%0" + session.getTotalKeysLength() + "d";
return String.format(format, (int) token).getBytes();
```
