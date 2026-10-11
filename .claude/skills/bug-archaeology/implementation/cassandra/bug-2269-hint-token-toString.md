# Bug 2269: Hint Destination Token Printed as Raw Byte Array

**Commit**: 4764a379e2 · **Ticket**: CASSANDRA-4568

## Detection Rule

When serializing a token or key for display, verify that a `TokenFactory.toString()` call is used rather than a raw `new String(byteBuffer.array())` cast, which produces garbage output for non-UTF-8 token encodings.

## Code Smell

`new String(r.key.key.array())` applied to a token-encoded row key renders binary token bytes as garbage characters; the code also uses an inverted null check (`if (r.cf != null)` without `continue`) that makes the intent harder to read.

## Tags

- scope: cassandra
- category: wrong-serialization
- code-pattern: raw-bytes-as-string-token
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause

`listPendingHints()` serialized hint destination tokens by casting the raw byte-array representation of the row key to a `String`. For non-string partitioners (e.g. `RandomPartitioner`, `Murmur3Partitioner`), this produces unreadable output. The fix uses `Token.TokenFactory.toString(r.key.token)` for correct human-readable token serialization. Additionally, the null check was inverted: `if (r.cf != null)` should skip processing when null, not include it; it was rewritten using `if (r.cf == null) continue`.

## Example

```java
// BEFORE
for (Row r : rows) {
    if (r.cf != null) // inverted intent
        result.put(new String(r.key.key.array()), r.cf.getColumnCount()); // binary garbage
}
```

```java
// AFTER
Token.TokenFactory tokenFactory = StorageService.getPartitioner().getTokenFactory();
for (Row r : rows) {
    if (r.cf == null)
        continue;
    int count = r.cf.getColumnCount();
    if (count > 0)
        result.put(tokenFactory.toString(r.key.token), count);
}
```
