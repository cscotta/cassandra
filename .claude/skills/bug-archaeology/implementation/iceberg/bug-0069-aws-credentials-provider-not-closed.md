# Bug 0069: Custom AwsCredentialsProvider not closed when RESTSigV4AuthSession is closed

**Commit**: f2ed6a9dba16e2a7e7015e92c60b3749a385dd75 · **Ticket**: #15818

## Detection Rule

When a class accepts an externally-supplied dependency that implements `AutoCloseable` (or a provider-specific closeable interface) and stores it in a field, verify that the class's own `close()` method invokes `close()` on that dependency; failing to do so leaks the resource on every session teardown.

## Code Smell

A `close()` method that calls `delegate.close()` directly (closing only one known dependency) but never closes other stored fields such as `credentialsProvider`, even though those fields may implement `AutoCloseable` or `SdkAutoCloseable`.

## Tags

- scope: universal
- category: resource-leak
- code-pattern: close-not-on-all-paths
- severity: performance
- subsystem: aws/rest-auth

## Root Cause

`RESTSigV4AuthSession.close()` only forwarded the call to its `delegate` `AuthSession`, ignoring the `credentialsProvider` field entirely. If a caller supplied a custom `AwsCredentialsProvider` that is also `AutoCloseable` (e.g., one that holds a background refresh thread or HTTP connection pool), that resource was never released. The fix wraps both closeable members in a `CloseableGroup` so that `close()` on the session closes all owned resources in one call.

## Example

```java
// BEFORE
@Override
public void close() {
    delegate.close();  // credentialsProvider never closed
}
```

```java
// AFTER
@Override
public void close() {
    try {
        closeableGroup.close();  // closes delegate + credentialsProvider (if AutoCloseable)
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
}
// constructor registers both:
// closeableGroup.addCloseable(this.delegate);
// if (credentialsProvider instanceof AutoCloseable c) closeableGroup.addCloseable(c);
```
