# Bug 0947: NPE Serializing ErrorMessage with Null Message

**Commit**: 50095947e2 · **Ticket**: CASSANDRA-11167
**Subsystem**: Native Transport / Protocol
**Category**: Missing null / bounds check

---

## Root Cause
`ErrorMessage.Codec.encode` and `encodedSize` passed `err.getMessage()` directly to `CBUtil.writeString` / `CBUtil.sizeOfString`. `Throwable.getMessage()` is permitted to return `null`, and `CBUtil.writeString` does not handle a `null` argument — it would attempt to compute the string's UTF-8 byte length, triggering a `NullPointerException`. This NPE propagated up through the Netty pipeline, closing the connection instead of delivering a well-formed error response to the client.

### Before / After
```java
// BEFORE
CBUtil.writeString(err.getMessage(), dest);
int size = 4 + CBUtil.sizeOfString(err.getMessage());

// AFTER
String errorString = err.getMessage() == null ? "" : err.getMessage();
CBUtil.writeString(errorString, dest);
int size = 4 + CBUtil.sizeOfString(errorString);
```

---

## How to Spot
Every call site that writes a `String` derived from an exception's message into a wire buffer should null-check first, because `getMessage()` is documented as nullable for all `Throwable` subclasses. Review serialization code paths that call `CBUtil.writeString`, `DataOutputPlus.writeUTF`, or similar; any that use `exception.getMessage()` without a null guard are vulnerable to this pattern.

---

## Test
`ProtocolErrorTest` was added to verify that an exception with a `null` message is serialized without throwing.
