# Bug 1190: NullPointerException When Computing Digest for a Data ReadResponse

**Commit**: b16d8c9caa · **Ticket**: CASSANDRA-9460
**Subsystem**: src/java/org/apache/cassandra/db/ReadResponse.java, service/RowDigestResolver.java
**Category**: Missing null / bounds check

---

## Root Cause

`ReadResponse` carried either a row or a digest, but never both. When `RowDigestResolver` needed to compare responses it called `response.digest()` on a data response, which returned `null` because the digest had never been computed. The resolver then proceeded to compare `null` against a non-null digest, throwing a `NullPointerException`. The fix lazily computes and caches the digest on the `ReadResponse` itself the first time it is needed, using a `compareAndSet` to guard against concurrent computation, so the value is always non-null when the comparison proceeds.

### Before / After

```java
// BEFORE – digest() returns null for data responses; no null guard in resolver
public boolean isDigestQuery()
{
    return digest != null;
}
```

```java
// AFTER – digest lazily populated; null safe
public void setDigest(ByteBuffer digest)
{
    ByteBuffer curr = this.digest;
    if (!digestUpdater.compareAndSet(this, curr, digest))
    {
        assert digest.equals(this.digest) : ...;
    }
}

public boolean isDigestQuery()
{
    return digest != null && row == null;
}
```

---

## How to Spot

Whenever an object has multiple construction paths (row-only vs. digest-only), any method that reads a field set by only one path will return null on the other path. Review every getter on such a "union" type and ask what happens when the complementary constructor was used. Lazy computation with a null-check plus a `compareAndSet` is the standard remedy when the value is expensive to compute upfront and may be needed on both paths.

---

## Test

No test added.
