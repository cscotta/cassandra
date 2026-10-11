# Bug 0627: JBOD Disk Boundaries Use Pending Ranges When Replacing Node with Same Address

**Commit**: 50e6e721b2 · **Ticket**: CASSANDRA-14084
**Subsystem**: `src/java/org/apache/cassandra/db/DiskBoundaryManager.java`, `org/apache/cassandra/service/StorageService.java`
**Category**: Logic error in condition

---

## Root Cause

`DiskBoundaryManager.getDiskBoundaries` checked `StorageService.instance.isBootstrapMode()` to decide whether to use pending token ranges (for a node that hasn't yet received data) instead of the normal owned ranges. When replacing a node at the same IP address, the replacement node marks itself as `UP/NORMAL` locally from the start — it does not go through the bootstrap pending-ranges state — yet `isBootstrapMode()` can still return `true` during the replacement sequence. This caused `DiskBoundaryManager` to assign disk boundaries based on pending ranges rather than the real owned ranges, resulting in an unbalanced distribution of SSTables across JBOD disks. A secondary bug existed in `isReplacingSameAddress()`, which called `DatabaseDescriptor.getReplaceAddress().equals(...)` without a null guard; if `replace_address` was not configured, this threw a NullPointerException.

### Before / After

```java
// BEFORE – pending ranges used for same-address replacement; NPE on null
if (StorageService.instance.isBootstrapMode()) {
    localRanges = tmd.getPendingRanges(cfs.keyspace.getName(), ...);
}

public static boolean isReplacingSameAddress() {
    return DatabaseDescriptor.getReplaceAddress()
                             .equals(FBUtilities.getBroadcastAddress()); // NPE if null
}
```

```java
// AFTER – same-address replacement excluded from pending-range path
if (StorageService.instance.isBootstrapMode()
    && !StorageService.isReplacingSameAddress()) {
    localRanges = tmd.getPendingRanges(cfs.keyspace.getName(), ...);
}

public static boolean isReplacingSameAddress() {
    InetAddress replaceAddress = DatabaseDescriptor.getReplaceAddress();
    return replaceAddress != null
           && replaceAddress.equals(FBUtilities.getBroadcastAddress());
}
```

---

## How to Spot

When a method like `isBootstrapMode()` is used as a guard to choose between two code paths, reviewers should enumerate all startup scenarios (normal bootstrap, same-address replacement, different-address replacement) and verify the guard produces the correct boolean in each. Any method that retrieves an optional configuration value and immediately calls a method on the result without a null check is a latent NPE waiting to surface.

---

## Test

No test added in this commit.
