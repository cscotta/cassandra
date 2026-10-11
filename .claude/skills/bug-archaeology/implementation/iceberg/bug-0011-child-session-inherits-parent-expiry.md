# Bug 0011: Child AuthSession inheriting parent's token expiry instead of its own

**Commit**: 57409faedb184eb3b410a9cbbab52a4e5c0b6f0a · **Ticket**: #15999

## Detection Rule

When a child object is built using `.from(parent)` or a similar copy-from-parent pattern AND the child represents a new independently-issued credential, verify that time-sensitive fields (expiry, issue time, TTL) are explicitly set from the child's own data rather than inherited from the parent.

## Code Smell

A builder call that uses `.from(parent.config())` to seed fields, followed by `.token(newToken)` and `.tokenType(...)`, but no `.expiresAtMillis(...)` — the expiry silently carries over from the parent config even though a new token was issued.

## Tags

- scope: universal
- category: stale-state-after-lifecycle
- code-pattern: stale-state-after-copy-from
- severity: silent-wrong-result
- subsystem: rest-auth / OAuth2

## Root Cause

`AuthSession.fromAccessToken`, `fromTokenResponse`, and the internal `refresh` method all start child/refreshed session config from `parent.config()` via the builder's `.from()` method. This copies `expiresAtMillis` from the parent, so the child session carries the parent token's expiry even after a completely new token with a different lifetime is issued. The fix is to call `.expiresAtMillis(OAuth2Util.expiresAtMillis(newToken))` explicitly on every code path that creates a new session.

## Example

```java
// BEFORE
AuthConfig childConfig = AuthConfig.builder()
    .from(parent.config())
    .token(response.token())
    .tokenType(response.issuedTokenType())
    // expiresAtMillis silently inherited from parent
    .build();
```

```java
// AFTER
AuthConfig childConfig = AuthConfig.builder()
    .from(parent.config())
    .token(response.token())
    .tokenType(response.issuedTokenType())
    .expiresAtMillis(OAuth2Util.expiresAtMillis(response.token()))
    .build();
```
