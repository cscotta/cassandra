**Commit**: c044eb61a1 · **Ticket**: KAFKA-18593
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
    val controllerId = {
      metadataCache.getControllerId.flatMap {
        case ZkCachedControllerId(id) => Some(id)
        case KRaftCachedControllerId(_) => metadataCache.getRandomAliveBrokerId
      }
    }
        metadataCache.getControllerId match {
          case Some(value) =>
            value match {
              case ZkCachedControllerId (id) => id
              case KRaftCachedControllerId (_) => metadataCache.getRandomAliveBrokerId.getOrElse(- 1)
            }

// After
    val controllerId = metadataCache.getRandomAliveBrokerId
        metadataCache.getRandomAliveBrokerId.getOrElse(-1)
import kafka.server.MetadataCache
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18593 remove zkcachedcontrollerid in metadatacache (#18625).

**Test**
No test added.
