**Commit**: c527530e80 · **Ticket**: KAFKA-19042
**Subsystem**: clients/clients-integration-tests/src/test/java/org/apache/kafka/clients/producer/ProducerCompressionTest.java
**Category**: Wrong constant / default value

**Root cause**: This commit is a pure test migration — `ProducerCompressionTest`, `ProducerFailureHandlingTest`, and `ProducerIdExpirationTest` were moved from the Scala integration-test module to the new Java `clients-integration-tests` module. No production logic changed. The migration also adds both classic and consumer protocol variants to compression testing.

**Before/After**:
```java
// No production code changed.
// Tests previously in core Scala module; now in:
// clients/clients-integration-tests/src/test/java/
//   org/apache/kafka/clients/producer/ProducerCompressionTest.java
```

**How to spot**: Pure test migration. No logic bug.

**Test**: `ProducerCompressionTest.testCompression` iterates over all `CompressionType` values, produces 2000 records with key/value/header permutations, and asserts correct offset ordering and payload fidelity. Both classic and consumer protocol consumers are tested per compression codec.
