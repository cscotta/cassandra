**Commit**: f81853ca88 · **Ticket**: KAFKA-19441

**Subsystem**: Group Coordinator / CoordinatorRuntime

**Category**: Abstraction / interface introduction

**Root cause**
`GroupCoordinator` was directly coupled to `MetadataImage`, a concrete KRaft class, for all metadata lookups. This prevented unit-testing the coordinator without a full `MetadataImage` object and made it impossible to substitute alternate metadata sources. Introducing the `CoordinatorMetadataImage` and `CoordinatorMetadataDelta` interfaces decouples the coordinator from the KRaft implementation detail; production code passes a `MetadataImageCoordinatorMetadataImage` adapter, while tests can inject lightweight fakes.

**Before/After**
```java
// Before: direct MetadataImage dependency
public class GroupCoordinator {
    private MetadataImage image;
    void onMetadataUpdate(MetadataImage newImage) { this.image = newImage; }
}

// After: interface-based dependency
public interface CoordinatorMetadataImage { ... }

public class GroupCoordinator {
    private CoordinatorMetadataImage image;
    void onMetadataUpdate(CoordinatorMetadataImage newImage) { this.image = newImage; }
}
```

**How to spot**
When coordinator or runtime classes import concrete KRaft types (`MetadataImage`, `MetadataDelta`) as fields rather than via interfaces, testability and layering are compromised. Introduce narrow interfaces that expose only the operations the coordinator actually needs.

**Test**
`GroupCoordinatorTest` and `CoordinatorRuntimeTest` use stub implementations of `CoordinatorMetadataImage` to verify metadata-update handling without constructing real `MetadataImage` instances.
