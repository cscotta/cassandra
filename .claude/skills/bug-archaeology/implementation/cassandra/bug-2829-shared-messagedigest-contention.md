# Bug 2829: Thread contention from shared MessageDigest in FBUtilities.hash

**Commit**: 671de68026 · **Ticket**: CASSANDRA-1369

## Detection Rule
When `MessageDigest.getInstance(type)` is called on every hash invocation, or a single `MessageDigest` is shared across threads, replace it with a `ThreadLocal<MessageDigest>` initialized once per thread to eliminate lock contention.

## Code Smell
`FBUtilities.hash(String type, ByteBuffer... data)` calls `MessageDigest.getInstance(type)` on every invocation, which acquires a JVM-level lock on the security provider. Under high concurrency (e.g., token computation during partitioning), this becomes a hot bottleneck.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: shared-messagedigest-contention
- severity: performance
- subsystem: storage-engine

## Root Cause
`MessageDigest.getInstance()` synchronizes on the JCA provider registry. Under many concurrent threads computing tokens or digests, this creates contention. The fix introduces a `ThreadLocal<MessageDigest>` that creates one MD5 instance per thread; each call then does only a `reset()` before use, avoiding all cross-thread locking.

## Example
```java
// BEFORE
public static byte[] hash(String type, ByteBuffer... data) {
    MessageDigest messageDigest = MessageDigest.getInstance(type);
    for (ByteBuffer block : data) messageDigest.update(...);
    return messageDigest.digest();
}

// AFTER
private static final ThreadLocal<MessageDigest> localMessageDigest =
    ThreadLocal.withInitial(() -> MessageDigest.getInstance("MD5"));

public static byte[] hash(ByteBuffer... data) {
    MessageDigest messageDigest = localMessageDigest.get();
    messageDigest.reset();
    for (ByteBuffer block : data) messageDigest.update(...);
    return messageDigest.digest();
}
```
