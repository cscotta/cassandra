**Commit**: 3922cadc5d · **Ticket**: KAFKA-9738
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Implements KIP-1070, which deprecates:
 - Transformer
 - TransformerSupplier
 - ValueTransformer
 - ValueTransformerSupplier
 - MockProcessorContext

Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.streams.kstream.Transformer;
 * Demonstrate the use of {@link MockProcessorContext} for testing the {@link Transformer} in the {@link WordCountTransformerDemo}.
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.ValueTransformer;
     * {@link Transformer Transformers},
     * or {@link ValueTransformer ValueTransformers} before they can be used.
     * {@link Transformer Transformers},
     * or {@link ValueTransformer ValueTransformer}; those have read-only access to all global stores by default.
     * {@link Transformer Transformers}, or {@link ValueTransformer ValueTransformer}; those have read-only access to all global stores by default.

// After
    <h3><a id="streams_api_changes_400" href="#streams_api_changes_400">Streams API changes in 4.0.0</a></h3>

    <p>
        In previous release, a new version of the Processor API was introduced and the old Processor API was
        incrementally replaced and deprecated.
        <a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-1070%3A+deprecate+MockProcessorContext">KIP-1070</a>
        follow this path by deprecating <code>MockProcessorContext</code>, <code>Transformer</code>,
        <code>TransformerSupplier</code>, <code>ValueTransformer</code>, and <code>ValueTransformerSupplier</code>.
    </p>

 * Demonstrate the use of {@link MockProcessorContext} for testing the {@link org.apache.kafka.streams.kstream.Transformer} in the {@link WordCountTransformerDemo}.
     * {@link org.apache.kafka.streams.kstream.Transformer Transformers},
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-9738: deprecate old processor api (#16742).

**Test**
Test modified in WordCountTransformerTest.java covering the corrected behaviour.
