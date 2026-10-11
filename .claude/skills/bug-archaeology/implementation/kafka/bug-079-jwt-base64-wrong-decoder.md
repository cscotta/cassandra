**Commit**: 78c1da1aca · **Ticket**: KAFKA-17089
**Subsystem**: clients/src/main/java/org/apache/kafka/common/security/oauthbearer/internals/unsecured/OAuthBearerUnsecuredJws.java
**Category**: Wrong serialization / deserialization

**Root cause**: `OAuthBearerUnsecuredJws.toMap()` decoded the Base64-encoded JWT sections using `Base64.getDecoder()` (the standard alphabet) instead of `Base64.getUrlDecoder()` (the URL-safe alphabet). JWT sections are defined by RFC 7519 to use URL-safe Base64 without padding; the standard decoder rejects characters such as `-` and `_` that appear in URL-safe encoding, throwing `IllegalArgumentException`. Server-side validation delegates to the jose4j library which handles this correctly, so only the client-side path was broken.

**Before/After**

```java
// Before
byte[] decode = Base64.getDecoder().decode(split);

// After
byte[] decode = Base64.getUrlDecoder().decode(split);
```

**How to spot**: Any JWT parsing code that directly calls `Base64.getDecoder()` rather than `Base64.getUrlDecoder()` will fail for payloads containing URL-safe characters. The bug is silent when the encoded content happens to avoid `-` and `_`, which can mask it in common test tokens.

**Test**: Constructs a JWT whose payload contains `>>>___<<<---` (characters that are legal in URL-safe Base64 but illegal in standard Base64). Verifies that `Base64.getDecoder()` throws `IllegalArgumentException` on the URL-encoded payload while `Base64.getUrlDecoder()` succeeds, and that the `ClientJwtValidator` validates the token without throwing.
