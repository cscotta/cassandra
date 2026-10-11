# Bug 0615: isCrossNode() Compares InetAddressAndPort to InetAddress, Always Returns True

**Commit**: 3fc381d5cb · **Ticket**: n/a
**Subsystem**: `src/java/org/apache/cassandra/net/MessageIn.java`
**Category**: Logic error in condition

---

## Root Cause

`MessageIn.isCrossNode()` compared `from` (an `InetAddressAndPort`) to `DatabaseDescriptor.getBroadcastAddress()` (a plain `InetAddress`) using `equals`. Because the types are different, `equals` always returned `false`, so every message — including those originating from the local node — was treated as a cross-node message. The fix unwraps `from.address` before comparing, making the type match. (This commit is an intermediate fix; CASSANDRA-14225 later further upgraded the comparison to use the port-aware `FBUtilities.getBroadcastAddressAndPort()`.)

### Before / After

```java
// BEFORE
public boolean isCrossNode() {
    return !from.equals(DatabaseDescriptor.getBroadcastAddress());
}
```

```java
// AFTER
public boolean isCrossNode() {
    return !from.address.equals(DatabaseDescriptor.getBroadcastAddress());
}
```

---

## How to Spot

Type-mismatch `equals` calls are a classic silent bug: the JVM happily calls `Object.equals`, which compares references or delegates to the left-hand type's implementation, neither of which throws an exception. A reviewer should grep for `equals(DatabaseDescriptor.get` or similar patterns and verify the argument type matches the receiver type. IDE type-mismatch warnings or FindBugs/SpotBugs `EC_UNRELATED_TYPES` would catch this automatically.

---

## Test

No test added.
