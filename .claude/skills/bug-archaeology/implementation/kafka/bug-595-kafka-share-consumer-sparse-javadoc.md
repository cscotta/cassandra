**Commit**: fb56f8a981 · **Ticket**: KAFKA-19364

**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/KafkaShareConsumer.java

**Category**: Missing null/bounds check

**Root cause**
The `KafkaShareConsumer` public API had sparse Javadoc compared to `KafkaConsumer`: many method contracts (subscribe, poll, acknowledge, commitSync) lacked descriptions of the share-consumer-specific semantics (e.g. the meaning of `AcknowledgeType.RELEASE`, the interaction with `max.delivery.count`, the behaviour of `SHARE_ACKNOWLEDGEMENT_MODE`). Users migrating from `KafkaConsumer` to `KafkaShareConsumer` would find the documentation insufficient for correct usage. The fix adds comprehensive Javadoc to 93 lines across all major `KafkaShareConsumer` API methods.

**Before**
```java
// Before: minimal Javadoc
/** Subscribe to the given topics. */
public void subscribe(Collection<String> topics) { ... }
```

**After**
```java
// After: complete Javadoc including share-consumer semantics
/**
 * Subscribe to the given topics for consuming from a share group...
 * Records are acquired from the share partition and must be
 * acknowledged via {@link #acknowledge(ConsumerRecord, AcknowledgeType)}
 * or committed via {@link #commitSync()}...
 */
public void subscribe(Collection<String> topics) { ... }
```

**How to spot**
Run `./gradlew javadoc` and look for `missing @return` or missing parameter descriptions on `KafkaShareConsumer` methods; any public method that lacks share-group-specific notes about delivery counts or acknowledgement modes is a documentation gap.

**Test**
Generate the Javadoc and confirm no `missing tag` warnings appear for `KafkaShareConsumer` methods updated in this commit.
