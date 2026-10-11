# Bug 2961: TokenUpdateVerbHandler deserializes token bytes directly instead of using serializer

**Commit**: 71b3b3c736 · **Ticket**: CASSANDRA-363

## Detection Rule
When deserializing a network message body that was serialized with a typed serializer (e.g., `Token.serializer().serialize()`), check whether the receiver uses the matching deserializer — calling a raw `fromByteArray()` on the message body when the body has a framing header will silently produce a wrong or truncated token.

## Code Smell
`TokenUpdateVerbHandler.doVerb` calls `StorageService.getPartitioner().getTokenFactory().fromByteArray(body)` directly on the raw message body, but the sender serialized the token using `Token.serializer()` which prepends metadata (type info or length prefix) not present in a raw `fromByteArray` call.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: raw-bytes-vs-serializer-mismatch
- severity: silent-wrong-result
- subsystem: network-messaging

## Root Cause
The `Token` class has two serialization paths: `getTokenFactory().toByteArray()/fromByteArray()` for raw bytes, and `Token.serializer().serialize()/deserialize()` which wraps the bytes in a framing format. The verb handler used `fromByteArray()` to read a token that was encoded with the full `Token.serializer()` framing, causing incorrect token parsing. Additionally, the old code swallowed `IOException` silently (just debug-logging it), hiding deserialization failures; the fix propagates them as `RuntimeException`.

## Example
```java
// BEFORE: raw byte-array parse ignores serializer framing
byte[] body = message.getMessageBody();
Token token = StorageService.getPartitioner().getTokenFactory().fromByteArray(body);
```
```java
// AFTER: use matching serializer
DataInputBuffer bufIn = new DataInputBuffer();
bufIn.reset(body, body.length);
Token token = Token.serializer().deserialize(bufIn);
```
