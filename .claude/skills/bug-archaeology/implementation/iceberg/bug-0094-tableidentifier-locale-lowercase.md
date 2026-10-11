# Bug 0094: TableIdentifier.toLowerCase uses default locale for namespace levels

**Commit**: beef60d95e94902eaaf2fac031194f6b2e038c6c · **Ticket**: #15956 / #15958

## Detection Rule

When a method performs `String.toLowerCase()` (no-arg) or `String.toUpperCase()` (no-arg) on identifiers, catalog names, field names, or any value that must be locale-independent, verify it uses `String.toLowerCase(Locale.ROOT)` instead, because the default-locale variant produces locale-specific results (e.g. Turkish "I" → "ı").

## Code Smell

`Arrays.stream(...).map(String::toLowerCase)` — a method reference to the no-argument `toLowerCase()` in a context where a sibling call on `name()` already uses `toLowerCase(Locale.ROOT)`, revealing an inconsistent treatment.

## Tags

- scope: java
- category: wrong-constant
- code-pattern: locale-sensitive-case-fold
- severity: silent-wrong-result
- subsystem: api/TableIdentifier

## Root Cause

`TableIdentifier.toLowerCase()` called `String.toLowerCase(Locale.ROOT)` for the table name but used the no-arg `String::toLowerCase` method reference for each namespace level. On JVMs running under a locale where ASCII letters fold differently (e.g., Turkish where `'I'.toLowerCase()` → `'ı'`), namespace levels were lowercased differently from the table name, causing identifier comparisons and catalog lookups to fail silently.

## Example

```java
// BEFORE
public TableIdentifier toLowerCase() {
    String[] newLevels =
        Arrays.stream(namespace().levels()).map(String::toLowerCase).toArray(String[]::new);
    String newName = name().toLowerCase(Locale.ROOT);
    return TableIdentifier.of(Namespace.of(newLevels), newName);
}
```

```java
// AFTER
public TableIdentifier toLowerCase() {
    String[] newLevels =
        Arrays.stream(namespace().levels())
            .map(s -> s.toLowerCase(Locale.ROOT))
            .toArray(String[]::new);
    String newName = name().toLowerCase(Locale.ROOT);
    return TableIdentifier.of(Namespace.of(newLevels), newName);
}
```
