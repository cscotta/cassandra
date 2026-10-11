# Bug 2878: SimpleAuthenticator MD5 comparison hashes the wrong operand

**Commit**: 6006466c73 · **Ticket**: CASSANDRA-1447

## Detection Rule
When verifying a password stored as a hash, confirm that the code hashes the user-supplied plaintext and compares against the stored hash — not the reverse.

## Code Smell
`MessageDigest.isEqual(password.getBytes(), MessageDigest.getInstance("MD5").digest(props.getProperty(username).getBytes()))` hashes the stored property file value and compares it to the raw plaintext bytes, which means authentication always fails unless the user literally submits the MD5 hash as their password.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: hash-operands-swapped
- severity: crash
- subsystem: auth-security

## Root Cause
The arguments to `MessageDigest.isEqual` were transposed: the stored property was being hashed instead of the submitted password. The fix hashes the user-submitted `password` bytes and compares the result against the hex-decoded stored hash via `FBUtilities.hexToBytes(props.getProperty(username))`.

## Example
```java
// BEFORE — hashes stored value, compares against plaintext input
authenticated = MessageDigest.isEqual(
    password.getBytes(),
    MessageDigest.getInstance("MD5").digest(props.getProperty(username).getBytes()));
```
```java
// AFTER — hashes submitted password, compares against stored hex hash
authenticated = MessageDigest.isEqual(
    MessageDigest.getInstance("MD5").digest(password.getBytes()),
    FBUtilities.hexToBytes(props.getProperty(username)));
```
