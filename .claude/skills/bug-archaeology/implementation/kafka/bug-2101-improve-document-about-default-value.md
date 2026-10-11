**Commit**: f7f4e5b5b1 · **Ticket**: KAFKA-17882
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
#### Default Value Handling for Tagged Fields

In Kafka's serialization mechanism, a tagged field may be omitted from the serialized message
if all its associated fields are equal to their default values, whether those defaults are explicit
or implicit. This behavior optimizes message size by avoiding the transmission of redundant data.

```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17882 improve the document about "default value" of tagged structure field (#17618).

**Test**
No test added.
