# Bug 0386: ETag cache collides across different snapshot loading modes

**Commit**: d23b0d39bfd0a1f914aaa6d24d65906fc99200a3 · **Ticket**: #15057

## Detection Rule

When an ETag or cache key is derived from a resource's location, verify that all request parameters that affect the response content are included in the hash, so that two requests for the same URL with different query parameters cannot produce the same ETag.

## Code Smell

A hash function that takes only the metadata file location string as input, used as an HTTP ETag, while a `?snapshots=refs` vs `?snapshots=all` query parameter changes which snapshot data is included in the response — so both modes hash to the same ETag and the client incorrectly treats the cached entry as fresh.

## Tags

- scope: distributed-systems
- category: wrong-filter-result
- code-pattern: serialize-size-mismatch
- severity: silent-wrong-result
- subsystem: core/rest-catalog

## Root Cause

`ETagProvider.of()` only hashed the metadata file location, ignoring query parameters. The REST catalog varies the `snapshots` query parameter between `all` and `refs` modes; a client that first loaded with `snapshots=all` would receive an ETag, then when using `snapshots=refs` the server would compute the same ETag and return HTTP 304 Not Modified, causing the client to silently use a response with the full snapshot list when it should have gotten only refs.

## Example

```java
// BEFORE
public static String of(String metadataLocation) {
    return MURMUR3.hashString(metadataLocation, StandardCharsets.UTF_8).toString();
}
```

```java
// AFTER
public static String of(String metadataLocation, Map<String, String> params) {
    String stringToHash = metadataLocation;
    if (params != null && !params.isEmpty()) {
        Map<String, String> orderedParams = new TreeMap<>(params);
        stringToHash = COMMA.join(metadataLocation, PARAMS_JOINER.join(orderedParams));
    }
    return MURMUR3.hashString(stringToHash, StandardCharsets.UTF_8).toString();
}
```
