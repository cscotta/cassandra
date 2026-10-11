# Bug 1819: Race Condition Referencing tokenMetadataCache

**Commit**: a3d91dc9d6 · **Ticket**: CASSANDRA-6485
**Subsystem**: src/java/org/apache/cassandra/locator
**Category**: Race condition / missing synchronization

---

## Root Cause

`AbstractReplicationStrategy.getCachedEndpoints` performed a double-checked locking pattern on `tokenMetadataClone`, but after the outer null-check passed and before the synchronized block was entered, another thread could set `tokenMetadataClone` back to null (invalidating the cache). The code then continued to dereference the field directly via `tokenMetadataClone.sortedTokens()` and `calculateNaturalEndpoints(..., tokenMetadataClone)` outside the synchronized block, using a potentially-null reference. The fix captures the field into a local variable `tm` atomically within the check-then-act sequence, so all subsequent dereferences use the locally captured, non-null reference.

### Before / After

```java
// BEFORE – field read outside sync; another thread can null it between check and use
if (tokenMetadataClone == null)
{
    synchronized (this)
    {
        if (tokenMetadataClone == null)
            tokenMetadataClone = tokenMetadata.cloneOnlyTokenMap();
    }
    keyToken = TokenMetadata.firstToken(tokenMetadataClone.sortedTokens(), searchToken);
}
endpoints = new ArrayList<>(calculateNaturalEndpoints(searchToken, tokenMetadataClone));
```

```java
// AFTER – local variable tm captures reference atomically; immune to concurrent nulling
TokenMetadata tm;
if ((tm = tokenMetadataClone) == null)
{
    synchronized (this)
    {
        if ((tm = tokenMetadataClone) == null)
            tm = tokenMetadataClone = tokenMetadata.cloneOnlyTokenMap();
    }
    keyToken = TokenMetadata.firstToken(tm.sortedTokens(), searchToken);
}
endpoints = new ArrayList<>(calculateNaturalEndpoints(searchToken, tm));
```

---

## How to Spot

Double-checked locking is only safe when every post-synchronization access to the shared field goes through a locally captured reference. Any code that re-reads the field after leaving the synchronized block is vulnerable to a concurrent null-out. Flag patterns of the form: check field → sync → use field (rather than use local).

---

## Test

No test added. The race is timing-dependent and was diagnosed from production observations (CASSANDRA-6345 / 6485).
