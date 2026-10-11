# Bug 0546: SASL Credential Parser Throws RuntimeException for Empty Username or Password

**Commit**: 177a8e91e3 · **Ticket**: CASSANDRA-15198
**Subsystem**: `src/java/org/apache/cassandra/auth/PasswordAuthenticator.java`
**Category**: Missing null / bounds check

---

## Root Cause

The SASL PLAIN credential parser iterated the token byte array backwards, scanning for NUL delimiters to split out the username and password segments. It set `user` and `pass` only when it found two NUL bytes and checked `user == null` / `pass == null` afterwards. If a client sent an empty username or empty password (a zero-length segment between two NUL bytes), `Arrays.copyOfRange` returned a non-null zero-length array, bypassing the null checks entirely and passing an empty string to bcrypt — which could cause a `RuntimeException`. Additionally the parser did not reject the case of more than two NUL bytes (malformed credential), leaving an extra field silently ignored.

### Before / After

```java
// BEFORE
if (user == null)
    throw new AuthenticationException("Authentication ID must not be null");
if (pass == null)
    throw new AuthenticationException("Password must not be null");
```

```java
// AFTER
// Also added: if a third NUL is seen, throw immediately
if (user == null || user.length == 0)
    throw new AuthenticationException("Authentication ID must not be null");
if (pass == null || pass.length == 0)
    throw new AuthenticationException("Password must not be null");
```

---

## How to Spot

When parsing a delimited protocol field, "found / not found" and "found but empty" are two different error conditions, yet a `null` check only catches the first. After any `copyOfRange` or `split`, verify that a zero-length result is also treated as an error when the spec requires a non-empty value. Additionally, look for parsers that stop after filling the expected number of fields without checking that no extra delimiters remain.

---

## Test

New parameterised tests cover empty username, empty password, NUL characters inside username or password, and all-NUL credentials, asserting each throws `AuthenticationException`.
