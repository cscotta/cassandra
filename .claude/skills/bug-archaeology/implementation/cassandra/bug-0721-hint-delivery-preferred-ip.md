# Bug 0721: Hint delivery resolves preferred IP instead of using canonical host ID

**Commit**: 2e36eb6ca3 · **Ticket**: CASSANDRA-13020
**Subsystem**: Hints / StorageService
**Category**: Logic error in condition

---

## Root Cause
When `prefer_local` is enabled in the endpoint snitch, Cassandra can map a host's broadcast address to a preferred (local) address for intra-DC communication. In the hint delivery code path, after sorting candidates by proximity and selecting the closest peer (`hintsDestinationHost`), the code called `SystemKeyspace.getPreferredIP(hintsDestinationHost)` to look up the preferred IP, then looked up the host ID using that preferred IP. The problem is that `tokenMetadata` keys host IDs by the canonical broadcast address, not the preferred-IP alias. Looking up by the preferred address returned `null`, so hints were delivered to a null host ID or silently dropped rather than being dispatched to the correct node.

### Before / After
```java
// BEFORE
InetAddress hintsDestinationHost = candidates.get(0);
InetAddress preferred = SystemKeyspace.getPreferredIP(hintsDestinationHost);
return tokenMetadata.getHostId(preferred);   // preferred IP not in tokenMetadata

// AFTER
InetAddress hintsDestinationHost = candidates.get(0);
return tokenMetadata.getHostId(hintsDestinationHost);  // use canonical address
```

---

## How to Spot
Any time a "preferred" or "translated" address is computed and immediately used as a key in a data structure that was populated with the original address, check that both maps share the same key space. Snitch address translation is purely a transport-layer concern; higher-level registries (token metadata, host IDs) always track the canonical broadcast address.

---

## Test
No test added.
