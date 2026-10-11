# Bug 1517: Prevent SSP from erroring out when the yaml is yanked from under it

**Commit**: 7e71d01a03 · **Ticket**: CASSANDRA-7663
**Subsystem**: src/java/org/apache/cassandra/locator
**Category**: Logic error in condition

---

## Root Cause

`SimpleSeedProvider.getSeeds()` re-read the YAML configuration file on every invocation and threw an `AssertionError` if the file could not be opened (e.g., the file was deleted or became temporarily unavailable). Because the node's seed list is already known at startup, a transient YAML read failure during normal operation should not bring down the node. The fix separates the initial load (where failure is fatal) from subsequent refresh attempts (where failure is logged as a warning and the previously-cached seed list is returned).

### Before / After

```java
// BEFORE
public List<InetAddress> getSeeds()
{
    Config conf;
    try
    {
        conf = DatabaseDescriptor.loadConfig();
    }
    catch (Exception e)
    {
        throw new AssertionError(e);
    }
    // ... parse seeds from conf ...
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
