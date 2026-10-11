**Commit**: 2658f25238 · **Ticket**: KAFKA-19305

**Subsystem**: KRaft / MetadataImage

**Category**: Refactoring correctness (immutability)

**Root cause**
`ClientQuotaImage` and `TopicImage` were plain classes with mutable map fields. Converting them to Java records and wrapping their map fields with `Collections.unmodifiableMap` in the compact constructor enforces that instances are immutable after construction, preventing accidental modification of shared metadata state that could lead to inconsistent views across threads consulting the same `MetadataImage` snapshot.

**Before/After**
```java
// Before: mutable map field
public class TopicImage {
    private final Map<Integer, PartitionRegistration> partitions;
    // map could be modified externally if reference escapes
}

// After: record with unmodifiable map
public record TopicImage(String name, Uuid id,
                         Map<Integer, PartitionRegistration> partitions) {
    public TopicImage {
        partitions = Collections.unmodifiableMap(partitions);
    }
}
```

**How to spot**
Metadata snapshot classes that store maps without defensive copying or `unmodifiableMap` wrapping are unsafe when the same instance is read from multiple threads. Converting value-type metadata classes to records with compact-constructor validation is a systematic fix.

**Test**
`TopicImageTest` and `ClientQuotaImageTest` verify that attempts to mutate the wrapped maps throw `UnsupportedOperationException`.
