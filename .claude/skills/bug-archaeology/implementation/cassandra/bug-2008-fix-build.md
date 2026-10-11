# Bug 2008: Native Protocol Auth Classes Renamed, ByteBuffer Decoding Broken

**Commit**: 496035585a · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/transport/messages/
**Category**: Wrong serialization / deserialization

---

## Root Cause

The native protocol auth message classes were renamed from `SaslChallenge`/`SaslResponse` to `AuthChallenge`/`AuthResponse` to reflect CQL native protocol v2 terminology, but references in `Client.java`, `Message.java`, and `ServerConnection.java` were not updated, breaking the build. Additionally, the `decode()` methods in the original classes were reading a `ByteBuffer` from `CBUtil.readValue()` but passing the `ByteBuffer` object directly to the constructor instead of first extracting the raw `byte[]` content, meaning the token would contain incorrect data if the buffer had a non-zero position.

### Before / After

```java
// BEFORE (Message.java codec table)
AUTH_CHALLENGE (14, Direction.RESPONSE, SaslChallenge.codec),
AUTH_RESPONSE  (15, Direction.REQUEST,  SaslResponse.codec),

// BEFORE (AuthChallenge.decode)
return new SaslChallenge(CBUtil.readValue(body)); // ByteBuffer passed, not byte[]
```

```java
// AFTER
AUTH_CHALLENGE (14, Direction.RESPONSE, AuthChallenge.codec),
AUTH_RESPONSE  (15, Direction.REQUEST,  AuthResponse.codec),

// AFTER (AuthChallenge.decode)
ByteBuffer b = CBUtil.readValue(body);
byte[] token = new byte[b.remaining()];
b.get(token);
return new AuthChallenge(token);
```

---

## How to Spot

When renaming a class, verify all references are updated — codec registration tables, type-check switch statements, and assert conditions are easy to miss. Also check that `ByteBuffer` values returned from utility methods are fully drained into `byte[]` before use, especially in serialization codecs where partial buffer positions cause silent data corruption.

---

## Test

No test added.
