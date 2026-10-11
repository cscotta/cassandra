# Bug 2320: MessagingService.setVersion returns unboxed null, causing NullPointerException

**Commit**: aae0dfc92a · **Ticket**: n/a

## Detection Rule

When a method returns a primitive type (e.g., `int`) but delegates to a `Map.put()` that can return `null`, verify that the null case is handled before unboxing.

## Code Smell

A method declared as `Integer setVersion(...)` (or relying on auto-unboxing to `int`) that returns `versions.put(address, version)` directly, where `versions` is a `ConcurrentHashMap<InetAddress, Integer>` whose `put()` returns `null` for new keys.

## Tags

- scope: java
- category: missing-null-check
- code-pattern: map-put-null-unboxing
- severity: crash
- subsystem: network-messaging

## Root Cause

`MessagingService.setVersion()` was declared to return `Integer` (auto-unboxed to `int` at call sites). `ConcurrentHashMap.put()` returns `null` when the key is new (no previous mapping). Callers unboxing the return value would get a `NullPointerException`. The fix explicitly handles the null return and substitutes the newly-inserted value.

## Example

```java
// BEFORE
public Integer setVersion(InetAddress address, int version)
{
    return versions.put(address, version);  // returns null for first call per address
}
```

```java
// AFTER
public int setVersion(InetAddress address, int version)
{
    Integer v = versions.put(address, version);
    return v == null ? version : v;  // safe: return the new value if no prior mapping
}
```
