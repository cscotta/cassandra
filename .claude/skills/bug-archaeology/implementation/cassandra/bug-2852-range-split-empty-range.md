# Bug 2852: Range split creates empty range when token equals left bound

**Commit**: 9c5a6d5239 · **Ticket**: CASSANDRA-1700

## Detection Rule
When splitting a wrapping range at a token equal to the range's left bound, `createFrom(token)` must return `null` to indicate an empty left sub-range, rather than creating a zero-length `Range(left, left)`.

## Code Smell
`Range.createFrom(Token token)` returns `new Range(left, token)`. When `token.equals(left)`, this creates a degenerate `Range(X, X)` which, for a wrapping range, means "the entire ring" instead of an empty range. The split caller then adds a full-ring range to the result, sending the query to every node.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: empty-range-not-null
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`AbstractBounds.split` assumed `createFrom` returned non-null when the token is a valid split point. But for a wrapping range, if the split token equals the left endpoint, the resulting left sub-range is empty and should be skipped. The fix makes `Range.createFrom` return `null` for `token.equals(left)`, and the `split` method propagates that: `lb = createFrom(token); rb = lb != null && token.equals(right) ? null : new Range(token, right)`.

## Example
```java
// BEFORE (Range.createFrom)
public AbstractBounds createFrom(Token token) {
    return new Range(left, token); // creates Range(X,X) = full ring if token==left
}

// AFTER
public AbstractBounds createFrom(Token token) {
    if (token.equals(left))
        return null;
    return new Range(left, token);
}
```
