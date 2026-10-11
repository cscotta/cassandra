# Bug 2500: Compression chunk_length_kb parsed as bytes instead of kilobytes

**Commit**: d8f5842c0b · **Ticket**: CASSANDRA-3492

## Detection Rule

When a configuration parameter is documented and named as a kilobyte value (suffix `_kb`) but the parsing code omits the `* 1024` conversion, the stored value will be 1024x smaller than intended.

## Code Smell

`return Integer.parseInt(chLength)` in a method named `parseChunkLength` for a parameter named `chunk_length_kb` — missing the unit multiplication that the field name implies.

## Tags

- scope: cassandra
- category: wrong-constant
- code-pattern: missing-unit-multiplier-in-parse
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

`CompressionParameters.parseChunkLength()` parsed the `chunk_length_kb` value with `Integer.parseInt(chLength)` and returned it directly as bytes. Since the parameter is specified in kilobytes, the code should return `1024 * Integer.parseInt(chLength)`. Without the multiplier, a configured chunk length of `64` (meaning 64 KB) would be stored as 64 bytes — far too small, causing corrupt chunk boundaries.

## Example

```java
// BEFORE
private static Integer parseChunkLength(String chLength) throws ConfigurationException
{
    ...
    return Integer.parseInt(chLength);  // returns bytes, but input is in KB
}
```

```java
// AFTER
private static Integer parseChunkLength(String chLengthKB) throws ConfigurationException
{
    ...
    return 1024 * Integer.parseInt(chLengthKB);  // correctly converts KB to bytes
}
```
