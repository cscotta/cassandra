# Bug 2148: SimpleAuthorizer Example Crashes on EnumSet.copyOf With Empty Set

**Commit**: 706612e300 · **Ticket**: CASSANDRA-5072
**Subsystem**: examples/simple_authentication/src/org/apache/cassandra/auth/SimpleAuthorizer.java
**Category**: Logic error in condition

---

## Root Cause

The example `SimpleAuthorizer` called `EnumSet.copyOf(Permission.NONE)` to create an empty permission set. `EnumSet.copyOf` throws `IllegalArgumentException` when passed an empty collection because it cannot determine the element type from an empty input. The fix replaces all such calls with `EnumSet.noneOf(Permission.class)`, which is the correct idiom for an empty typed `EnumSet`. A secondary bug used `user.username` (a field that may have been package-private or removed) instead of the public accessor `user.getName()`.

### Before / After

```java
// BEFORE – throws IllegalArgumentException for empty collections
return EnumSet.copyOf(Permission.NONE);
EnumSet<Permission> authorized = EnumSet.copyOf(Permission.NONE);
if (admin.equals(user.username))
```

```java
// AFTER
return EnumSet.noneOf(Permission.class);
EnumSet<Permission> authorized = EnumSet.noneOf(Permission.class);
if (admin.equals(user.getName()))
```

---

## How to Spot

`EnumSet.copyOf(collection)` must never be called with an empty collection; it requires at least one element to infer the enum type. When initializing a permissions or flags set to "none", always use `EnumSet.noneOf(Type.class)`. Also, prefer accessor methods over direct field access on API types.

---

## Test

No test added.
