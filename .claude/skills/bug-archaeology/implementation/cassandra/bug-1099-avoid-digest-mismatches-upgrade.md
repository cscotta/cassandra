# Bug 1099: Avoid Digest Mismatches on Upgrade to 3.0

**Commit**: 782a1c3aec · **Ticket**: CASSANDRA-9554
**Subsystem**: src/java/org/apache/cassandra/db/ReadCommand.java, src/java/org/apache/cassandra/db/ReadResponse.java, src/java/org/apache/cassandra/db/rows/UnfilteredRowIterators.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

The 3.0 storage engine computes read-repair digests by serializing partitions using the 3.0 wire format. During a rolling upgrade, old (pre-3.0) replica nodes compute their digests using the legacy Thrift-derived format. Because the two formats differ, the coordinator saw a permanent digest mismatch between any 3.0 node and any pre-3.0 node on every read-repair attempt. The fix introduces a `digestVersion` field on `ReadCommand` that is propagated with the request: when a digest query is sent to a pre-3.0 node, the response carries the version the remote node used, and the local replica uses that same version when computing its own digest for comparison — so both sides hash identical byte sequences.

### Before / After

```java
// BEFORE — digest always computed with 3.0 format
public static void digest(UnfilteredRowIterator iterator, MessageDigest digest)
{
    // TODO: we're not computing digest the same way that old nodes ...
    digest.update(iterator.partitionKey().getKey().duplicate());
    ...
}
```

```java
// AFTER — format selected by requested messaging version
public static void digest(UnfilteredRowIterator iterator, MessageDigest digest, int version)
{
    if (version < MessagingService.VERSION_30)
    {
        LegacyLayout.fromUnfilteredRowIterator(iterator).digest(iterator.metadata(), digest);
        return;
    }
    digest.update(iterator.partitionKey().getKey().duplicate());
    ...
}
```

---

## How to Spot

Whenever digest or checksum computation crosses a version boundary, the exact byte sequence hashed must be version-aware. A `TODO` comment admitting "we're not computing the digest the same way as old nodes" is the clearest signal that a version-gated code path is missing. More broadly, any time two nodes hash the same logical data but use different wire serializers, mismatches are inevitable; review every `digest()` call to confirm it receives — and forwards — the correct protocol version.

---

## Test

A new `DigestBackwardCompatibilityTest` was added that verifies digest computation matches between legacy and 3.0 formats.
