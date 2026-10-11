# Bug 0832: Legacy 2.2 Temporary-File Regex Misses `lb` Format

**Commit**: a1d9a0295c · **Ticket**: CASSANDRA-12565
**Subsystem**: SSTable I/O / Descriptor
**Category**: Logic error in condition

---

## Root Cause
`Descriptor.LEGACY_TMP_REGEX` was written as `(la|ka)` which matches only the exact two-character strings `la` and `ka`. SSTable format codes from 2.2 follow the pattern `lb`, `lc`, ... (a letter followed by a letter), so any 2.2 temporary file whose format version was not `la` or `ka` was silently excluded from the legacy-file classification. The regex needed to match the character class `[lk]` followed by any single character, not a fixed two-character alternation.

### Before / After
```java
// BEFORE
private final static String LEGACY_TMP_REGEX_STR =
    "^((.*)\\-(.*)\\-)?tmp(link)?\\-(la|ka)\\-(\\d)*\\-(.*)$";

// AFTER
private final static String LEGACY_TMP_REGEX_STR =
    "^((.*)\\-(.*)\\-)?tmp(link)?\\-((?:l|k).)\\-(\\d)*\\-(.*)$";
```

---

## How to Spot
Whenever a regex enumerates version tokens that are actually a pattern (prefix letter + suffix letter), watch for hard-coded alternations that do not cover the full pattern space. The fix changes `(la|ka)` to `((?:l|k).)` — a non-capturing group matching `l` or `k` followed by any single character — which correctly handles all existing and future minor-version format codes under each major format family.

---

## Test
`SystemKeyspaceTest.testMigrateDataDirs_2_2` was updated to assert 7 legacy files instead of 5, and the test data directory was extended with two `tmp-lb-*` files that were invisible under the old regex.
