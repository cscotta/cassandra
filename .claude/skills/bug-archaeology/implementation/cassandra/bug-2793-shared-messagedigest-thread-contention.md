# Bug 2793: Shared MessageDigest Instance Causes Data Corruption Under Concurrency

**Commit**: 73ad88aeeb · **Ticket**: n/a

## Detection Rule
When a `MessageDigest` instance is shared across threads without synchronization and used to hash data for partitioner token assignment, concurrent invocations produce corrupted hash results.

## Code Smell
`FBUtilities.hash(String type, ByteBuffer... data)` calls `MessageDigest.getInstance(type)` on every call (allocating a new instance each time) but the same method signature is also used in a way where a cached single instance could be introduced without locking — the pre-fix call path suggests contention. The naming `md5hash` and a shared singleton-style usage pattern indicates the threading hazard.

## Tags
- scope: java
- category: race-condition
- code-pattern: shared-messagedigest-no-lock
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`MessageDigest` objects are not thread-safe. The `FBUtilities.hash()` method was allocating a new `MessageDigest` via `getInstance()` on every call, which itself acquires a provider lock and creates contention under high throughput. The fix introduces a `ThreadLocal<MessageDigest>` so each thread has its own private instance, eliminating both the lock contention and any risk of concurrent state corruption. The `reset()` call before use ensures the thread-local instance is clean for each hashing operation.

## Example
```java
// BEFORE: new instance per call, lock contention; easy to accidentally share
public static byte[] hash(String type, ByteBuffer... data) {
    MessageDigest messageDigest = MessageDigest.getInstance(type); // provider lock every call
    for (ByteBuffer block : data) messageDigest.update(...);
    return messageDigest.digest();
}
```
```java
// AFTER: thread-local instance, no contention, no sharing
private static final ThreadLocal<MessageDigest> localMessageDigest = new ThreadLocal<MessageDigest>() {
    protected MessageDigest initialValue() { return MessageDigest.getInstance("MD5"); }
};
public static byte[] hash(ByteBuffer... data) {
    MessageDigest messageDigest = localMessageDigest.get();
    messageDigest.reset();
    for (ByteBuffer block : data) messageDigest.update(...);
    return messageDigest.digest();
}
```
