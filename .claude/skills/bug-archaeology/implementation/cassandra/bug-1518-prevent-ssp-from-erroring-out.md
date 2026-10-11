# Bug 1518: Prevent SSP from erroring out when the yaml is yanked from under it

**Commit**: 73eb68c516 · **Ticket**: CASSANDRA-7663
**Subsystem**: src/java/org/apache/cassandra/locator
**Category**: Logic error in condition

---

## Root Cause

This is the same fix as commit 7e71d01a03 applied to a different branch. `SimpleSeedProvider.getSeeds()` directly opened the YAML config URL on every call and threw `AssertionError` if the stream could not be opened. A node that has already loaded its seed list successfully should not crash if the YAML file becomes unavailable later. The fix caches the seed list at construction time and degrades gracefully to a warning log on subsequent refresh failures, returning the last-known seed list.

### Before / After

```java
// BEFORE
public List<InetAddress> getSeeds()
{
    InputStream input;
    try
    {
        URL url = DatabaseDescriptor.getStorageConfigURL();
        input = url.openStream();
    }
    catch (Exception e)
    {
        throw new AssertionError(e);
    }
    // ... parse seeds ...
    return Collections.unmodifiableList(seeds);
}
```

```java
// AFTER
List<InetAddress> seeds; // cached at construction time

public List<InetAddress> getSeeds()
{
    try
    {
        seeds = loadSeeds();
    }
    catch (Exception e)
    {
        logger.warn("Could not refresh seeds from configuration file: {}", e);
    }
    return Collections.unmodifiableList(seeds);
}
```

---

## How to Spot

Any method that re-reads a configuration file at runtime and throws on failure should be audited to determine whether the data was already loaded at startup. If a cached value exists, a read-time failure should fall back to the cache rather than crashing, as the resource may be temporarily unavailable.

---

## Test

No test added.
