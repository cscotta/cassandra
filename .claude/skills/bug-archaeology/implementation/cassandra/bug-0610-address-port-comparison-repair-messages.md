# Bug 0610: Address-Only Comparison Ignores Port in Repair and Cross-Node Message Check

**Commit**: 834f2a6ecd · **Ticket**: CASSANDRA-14225
**Subsystem**: `src/java/org/apache/cassandra/net/MessageIn.java`, `repair/RepairRunnable.java`
**Category**: Logic error in condition

---

## Root Cause

Two separate comparisons checked only the IP address component of `InetAddressAndPort` objects instead of the full address-plus-port value. In `MessageIn.isCrossNode()`, `from` (an `InetAddressAndPort`) was compared against `DatabaseDescriptor.getBroadcastAddress()` (a plain `InetAddress`), so a message arriving on a non-default port from the same host would be misclassified as cross-node. In `RepairRunnable`, the trace-events loop filtered by `source.address.equals(r.getInetAddress("source"))`, ignoring `source_port`; when the port column was added to the trace schema, the repair event filtering silently stayed port-blind.

### Before / After

```java
// BEFORE – MessageIn.java
public boolean isCrossNode() {
    return !from.address.equals(DatabaseDescriptor.getBroadcastAddress());
}

// BEFORE – RepairRunnable.java
if (source.address.equals(r.getInetAddress("source")))
    continue;
```

```java
// AFTER – MessageIn.java
public boolean isCrossNode() {
    return !from.equals(FBUtilities.getBroadcastAddressAndPort());
}

// AFTER – RepairRunnable.java
int port = DatabaseDescriptor.getStoragePort();
if (r.has("source_port"))
    port = r.getInt("source_port");
InetAddressAndPort eventNode =
    InetAddressAndPort.getByAddressOverrideDefaults(r.getInetAddress("source"), port);
if (source.equals(eventNode))
    continue;
```

---

## How to Spot

When a codebase transitions from `InetAddress` to an `InetAddressAndPort` wrapper, search all existing comparison sites; many will still dereference `.address` or call the old `DatabaseDescriptor.getBroadcastAddress()` rather than the new port-aware equivalents. A reviewer should grep for `.address.equals(` and `getBroadcastAddress()` to find leftover partial comparisons.

---

## Test

No dedicated unit test added; the fix is structural correctness for mixed-version / non-default-port clusters.
