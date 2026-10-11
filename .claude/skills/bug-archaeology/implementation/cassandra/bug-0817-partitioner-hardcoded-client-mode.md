# Bug 0817: Partitioner Hardcoded in Client-Mode Initialization

**Commit**: cf5cd158e9 · **Ticket**: CASSANDRA-12677
**Subsystem**: Config / SSTable Writer
**Category**: Wrong constant / default value

---

## Root Cause
`DatabaseDescriptor.clientInitialization()` unconditionally set `partitioner = Murmur3Partitioner.instance`, which prevented callers (such as the `DatabaseDescriptorRefTest` class-loader isolation test) from supplying their own partitioner. The fix removes the hard-coded assignment from `clientInitialization()` and instead moves it into `CQLSSTableWriter`'s static initializer, where it becomes a conditional default: set `Murmur3Partitioner` only when no partitioner has been configured.

### Before / After
```java
// BEFORE — in DatabaseDescriptor.clientInitialization()
Config.setClientMode(true);
conf = new Config();
diskOptimizationStrategy = new SpinningDiskOptimizationStrategy();
partitioner = Murmur3Partitioner.instance;  // always clobbers any prior setting

// AFTER — partitioner line removed from clientInitialization();
// conditional default added to CQLSSTableWriter static block:
DatabaseDescriptor.clientInitialization(false);
if (DatabaseDescriptor.getPartitioner() == null)
    DatabaseDescriptor.setPartitionerUnsafe(Murmur3Partitioner.instance);
```

---

## How to Spot
When reviewing class-wide initialization methods (especially "client mode" or "tool mode" bootstraps), check whether they unconditionally assign values that other code paths — tests included — may need to set differently. Any hard-coded default that silently overwrites a caller-supplied value is a smell: the default should be conditional (`if (x == null) x = defaultValue`).

---

## Test
`DatabaseDescriptorRefTest` verifies that `DatabaseDescriptor` can be loaded in isolation without pulling in the full server stack. The pre-existing test failed because the forced `Murmur3Partitioner` import caused unwanted class initialization; removing the assignment fixes the isolation boundary.
