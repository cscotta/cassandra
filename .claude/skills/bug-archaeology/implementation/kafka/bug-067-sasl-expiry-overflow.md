**Commit**: ea771563e0 · **Ticket**: KAFKA-14604
**Subsystem**: clients/src/main/java/org/apache/kafka/common/security/authenticator/SaslClientAuthenticator.java
**Category**: Logic error in condition

**Root cause**
The SASL session expiration time was computed as:
```java
sessionExpirationTimeNanos = authenticationEndNanos + 1000 * 1000 * sessionLifetimeMs;
```
Both `1000 * 1000` and `sessionLifetimeMs` are `long` values. When `sessionLifetimeMs` is large (e.g. because `connections.max.reauth.ms` is set to `Long.MAX_VALUE`), the multiplication overflows silently to a negative number, producing an expiration timestamp in the past and causing immediate forced reconnection. The fix uses `Math.addExact` and a helper `Utils.msToNs` that calls `Math.multiplyExact`, which throws `ArithmeticException` on overflow instead of silently wrapping.

**Before/After**
```java
// Before: silent integer overflow
clientSessionReauthenticationTimeNanos =
    authenticationEndNanos + 1000 * 1000 * sessionLifetimeMsToUse;

// After: overflow detected and thrown
clientSessionReauthenticationTimeNanos =
    Math.addExact(authenticationEndNanos, Utils.msToNs(sessionLifetimeMsToUse));
```

**How to spot**
Any time a millisecond value is multiplied by 1,000,000 to convert to nanoseconds, check for overflow. For values approaching `Long.MAX_VALUE / 1_000_000` (~9.2 × 10^9 ms ≈ 290 years), the product overflows. Use `Math.multiplyExact` or cap the input before multiplication.

**Test**
`testReauthenticateWithLargeReauthValue` sets `connections.max.reauth.ms = Long.MAX_VALUE` on the server side and verifies the client channel is closed (due to the arithmetic error being caught), asserting 0 successful authentications and 0 successful reauths.
