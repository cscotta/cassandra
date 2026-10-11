# Bug 2243: Build Failure Due to Non-Final Variable Captured in Anonymous Class

**Commit**: fa9bbf7e19 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Logic error in condition

---

## Root Cause

A local variable `remotes` was captured inside an anonymous `Runnable` (or equivalent inner class) but was not declared `final`. Java requires that local variables referenced inside anonymous or inner classes be effectively final. The build failed because the compiler rejected the capture. Adding `final` to the declaration resolved the compilation error.

### Before / After

```java
// BEFORE
Set<InetAddress> remotes = Sets.difference(ImmutableSet.copyOf(targets), ImmutableSet.of(FBUtilities.getBroadcastAddress()));
```

```java
// AFTER
final Set<InetAddress> remotes = Sets.difference(ImmutableSet.copyOf(targets), ImmutableSet.of(FBUtilities.getBroadcastAddress()));
```

---

## How to Spot

When a local variable used inside an anonymous class or lambda is not declared `final` (pre-Java 8 "effectively final"), the Java compiler will refuse to compile the file. Any variable captured by an inner class should be declared `final` or not reassigned after initialization.

---

## Test

No test added.
