# Bug 2504: SSTable digest writer splits path with unescaped separator character

**Commit**: 5771a1c836 · **Ticket**: CASSANDRA-3456

## Detection Rule
When splitting a file path string using `String.split()`, pass the separator through `Pattern.quote()` to avoid treating it as a regex metacharacter.

## Code Smell
`str.split(new Character(File.separatorChar).toString())` — the character is converted to a string but not regex-escaped, so `\` on Windows is treated as an escape sequence in the regex engine and yields an empty result.

## Tags
- scope: universal
- category: wrong-constant
- code-pattern: unescaped-regex-metacharacter
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`String.split()` treats its argument as a regular expression. On Windows, `File.separatorChar` is `\`, which is a regex metacharacter. Passing the raw string `"\"` to `split()` causes a `PatternSyntaxException` or matches nothing, so the last path component is never extracted and the digest file receives a wrong filename entry.

## Example
```java
// BEFORE
String[] tmp = newdesc.filenameFor(SSTable.COMPONENT_DATA)
    .split(new Character(File.separatorChar).toString());
```
```java
// AFTER
String[] tmp = newdesc.filenameFor(SSTable.COMPONENT_DATA)
    .split(Pattern.quote(File.separator));
```
