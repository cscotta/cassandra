# Bug 0381: Path rewriting fails when path equals source prefix

**Commit**: f49b2fd97b48682d4e4ca6f1a552cb48f53c4ea5 · **Ticket**: #15172

## Detection Rule

When a string is stripped of a prefix by calling `startsWith(prefix)` and then `substring(prefix.length())`, verify that the case where the string equals the prefix exactly is handled and does not produce a stale or incorrect suffix.

## Code Smell

A path-prefix stripping method that normalizes the prefix to end with a separator (e.g. appends "/") before calling `startsWith`, but then calls `substring` on the original un-normalized path — so when the original path equals the prefix it yields an empty string from the un-normalized side, while `combinePaths` unconditionally appends a separator, producing a spurious trailing "/".

## Tags

- scope: universal
- category: off-by-one
- code-pattern: sentinel-not-handled
- severity: silent-wrong-result
- subsystem: core/table-migration

## Root Cause

`relativize()` normalized the prefix to end with "/" before calling `startsWith`, but then called `substring` on the original path (without the trailing "/"). When path equals prefix exactly, the normalized prefix is one character longer than the path, causing `startsWith` to return false and throw. Even if it had passed, `combinePaths` always appended a separator to the base before concatenating, turning an empty relative path into a spurious trailing slash.

## Example

```java
// BEFORE
public static String relativize(String path, String prefix) {
    String toRemove = maybeAppendFileSeparator(prefix); // "/src/"
    if (!path.startsWith(toRemove)) {                  // "/src".startsWith("/src/") == false → throws
        throw new IllegalArgumentException(...);
    }
    return path.substring(toRemove.length());
}

public static String combinePaths(String absolutePath, String relativePath) {
    return maybeAppendFileSeparator(absolutePath) + relativePath; // "/tgt/" + "" == "/tgt/"
}
```

```java
// AFTER
public static String relativize(String path, String prefix) {
    String toRemove = maybeAppendFileSeparator(prefix);
    String normalizedPath = maybeAppendFileSeparator(path);
    if (!normalizedPath.startsWith(toRemove)) {
        throw new IllegalArgumentException(...);
    }
    return normalizedPath.equals(toRemove) ? "" : path.substring(toRemove.length());
}

public static String combinePaths(String absolutePath, String relativePath) {
    return relativePath.isEmpty()
        ? absolutePath
        : maybeAppendFileSeparator(absolutePath) + relativePath;
}
```
