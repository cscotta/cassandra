# Bug 0205: propertiesWithPrefix treats prefix as regex, corrupts keys with metacharacters

**Commit**: e0fe281360281c0a9247858ca57e8b1099807718 · **Ticket**: #15558

## Detection Rule

When stripping a known literal prefix from a string, verify that the code uses `substring(prefix.length())` or an equivalent literal-only method rather than `replaceFirst(prefix, "")` or any other regex-based substitution that would interpret the prefix as a pattern.

## Code Smell

A call to `String.replaceFirst(prefix, "")` where `prefix` is an arbitrary user-supplied or configuration-derived string; any prefix containing regex metacharacters (`[`, `]`, `.`, `*`, `(`, etc.) will either silently produce a wrong result or throw `PatternSyntaxException`.

## Tags

- scope: java
- category: wrong-filter-result
- code-pattern: regex-used-for-literal
- severity: silent-wrong-result
- subsystem: core/util

## Root Cause

`PropertyUtil.propertiesWithPrefix` stripped the prefix from matching keys with `e.getKey().replaceFirst(prefix, "")`. `String.replaceFirst` treats its first argument as a regular expression. A prefix such as `prefix[0].` is interpreted as the regex `prefix[0].` (character class `[0]` followed by any character `.`), so the prefix is not stripped correctly from the key. For prefixes containing an unclosed bracket (e.g. `prefix[0.`), `replaceFirst` throws `PatternSyntaxException` at runtime, crashing callers.

## Example

```java
// BEFORE — replaceFirst interprets prefix as a regex
return properties.entrySet().stream()
    .filter(e -> e.getKey().startsWith(prefix))
    .collect(Collectors.toMap(
        e -> e.getKey().replaceFirst(prefix, ""),
        Map.Entry::getValue));
```

```java
// AFTER — substring uses literal prefix length
return properties.entrySet().stream()
    .filter(e -> e.getKey().startsWith(prefix))
    .collect(Collectors.toMap(
        e -> e.getKey().substring(prefix.length()),
        Map.Entry::getValue));
```
