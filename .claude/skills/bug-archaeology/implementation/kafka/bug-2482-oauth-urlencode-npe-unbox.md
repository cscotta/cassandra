**Commit**: dd7d7c3145 · **Ticket**: KAFKA-17335
**Subsystem**: clients/security/oauthbearer/AccessTokenRetrieverFactory
**Category**: Missing null/bounds check

**Root cause**
`AccessTokenRetrieverFactory` called `configs.get(SASL_OAUTHBEARER_HEADER_URLENCODE)` which returns `null` when the key is absent from the map. The return value was immediately auto-unboxed to a primitive `boolean`, causing a `NullPointerException` at runtime for any OAUTHBEARER configuration that omitted this optional property. The fix replaces the direct map access with a null-safe helper that provides the default value before unboxing.

**Before/After**
```java
// Before: NPE when key is absent
boolean headerUrlEncode = (boolean) configs.get(SASL_OAUTHBEARER_HEADER_URLENCODE);

// After: safe with default value
boolean headerUrlEncode = (boolean) configs.getOrDefault(
    SASL_OAUTHBEARER_HEADER_URLENCODE, false);
```

**How to spot**
Every `Map.get()` call whose result is immediately cast and unboxed to a primitive type is a latent NPE. Search for patterns like `(boolean) map.get(key)` or `(int) map.get(key)` and replace them with `getOrDefault` or an explicit null check before unboxing.

**Test**
No dedicated new test; existing OAuth integration tests exercise the code path with the key absent.
