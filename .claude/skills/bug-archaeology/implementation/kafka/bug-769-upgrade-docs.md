**Commit**: aa3a64e92c · **Ticket**: KAFKA-19668
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Docs change for KAFKA-19668 bug fix. Reviewers: Bill Bejeck <bill@confluent.io>, Lucas Brutschy
<lbrutschy@confluent.io>

**Before/After**
```java
// Before
                        <code>FixedKeyProcessor</code> (provided by a given <code>FixedKeyProcessorSupplier</code>);
                        <code>ProcessorContext</code> instance.
                        manages the <code>FixedKeyProcessorContext</code> instance.
            <li><a href="https://issues.apache.org/jira/browse/KAFKA-16339">"transformer" methods and classes in both Java and Scala</a></li>

// After
                        <code>FixedKeyProcessor</code> (provided by a given <code>FixedKeyProcessorSupplier</code>)
                        [<b>CAUTION:</b> If you are deploying a new Kafka Streams application, and you are using the
                        "merge repartition topics" optimization, you should enable the fix for
                        <a href="https://issues.apache.org/jira/browse/KAFKA-19668">KAFKA-19668</a> to avoid compatibility
                        issues for future upgrades to newer versions of Kafka Streams;
                        For more details, see the <a href="#transformers-removal-and-migration-to-processors">migration guide</a> below];
                        <code>ProcessorContext</code> instance;
                        manages the <code>FixedKeyProcessorContext</code> instance;

                <p><b>CAUTION:</b> If you are using <code>KStream.transformValues()</code> and you have the "merge repartition topics"
                    optimization enabled, rewriting your program to <code>KStream.processValues()</code> might not be safe due to
                    <a href="https://issues.apache.org/jira/browse/KAFKA-19668">KAFKA-19668</a>. For this case, you should not upgrade
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19668: update upgrade docs (#20484).

**Test**
No test added.
