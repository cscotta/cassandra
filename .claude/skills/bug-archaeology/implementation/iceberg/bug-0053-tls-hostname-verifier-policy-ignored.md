# Bug 0053: Custom hostname verifier silently double-verified by JSSE built-in verifier

**Commit**: a8b7ba6d5ffb47f3e6d4ba937e191c7648c0fe0f · **Ticket**: #15500

## Detection Rule

When a custom `HostnameVerifier` is passed to a TLS strategy that also accepts a `HostnameVerificationPolicy`, verify that the policy is explicitly set to `CLIENT` (or equivalent) so the built-in verifier is disabled and the custom verifier is the sole arbiter of hostname verification.

## Code Smell

A method returns a custom `HostnameVerifier` (e.g., `NoopHostnameVerifier`) but the `HostnameVerificationPolicy` parameter passed to the TLS strategy constructor is left at its default (`BUILTIN` or `BOTH`), so the custom verifier cannot override the built-in JSSE check.

## Tags

- scope: java
- category: wrong-constant
- code-pattern: policy-overrides-custom-handler
- severity: silent-wrong-result
- subsystem: core/rest/tls

## Root Cause

Apache HttpClient 5.4 added `HostnameVerificationPolicy` to `DefaultClientTlsStrategy`, distinguishing whether hostname verification is performed by the JSSE provider (`BUILTIN`), the HttpClient layer (`CLIENT`), or both (`BOTH`). The old code always passed `tlsConfigurer.hostnameVerifier()` directly to the strategy without setting the policy, so when a caller returned a custom verifier (e.g., `NoopHostnameVerifier`), the JSSE built-in verifier still ran first and rejected the connection — making the custom verifier unreachable. The fix infers the correct policy from whether a custom verifier is present: `CLIENT` when one is supplied, `BUILTIN` otherwise.

## Example

```java
// BEFORE — policy not set; JSSE built-in verifier runs regardless of custom verifier
connectionManagerBuilder.setTlsSocketStrategy(
    new DefaultClientTlsStrategy(
        tlsConfigurer.sslContext(),
        tlsConfigurer.supportedProtocols(),
        tlsConfigurer.supportedCipherSuites(),
        SSLBufferMode.STATIC,
        tlsConfigurer.hostnameVerifier()));
```

```java
// AFTER — policy derived from presence of custom verifier
HostnameVerifier customVerifier = tlsConfigurer.hostnameVerifier();
HostnameVerificationPolicy verificationPolicy =
    customVerifier != null
        ? HostnameVerificationPolicy.CLIENT
        : HostnameVerificationPolicy.BUILTIN;
connectionManagerBuilder.setTlsSocketStrategy(
    new DefaultClientTlsStrategy(
        tlsConfigurer.sslContext(),
        tlsConfigurer.supportedProtocols(),
        tlsConfigurer.supportedCipherSuites(),
        SSLBufferMode.STATIC,
        verificationPolicy,
        customVerifier));
```
