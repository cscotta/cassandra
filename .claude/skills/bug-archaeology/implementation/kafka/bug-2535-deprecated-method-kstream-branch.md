**Commit**: c3d552d273 · **Ticket**: KAFKA-12824
**Subsystem**: streams/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
The method branch in both Java and Scala KStream class was deprecated in version 2.8:

1) org.apache.kafka.streams.scala.kstream.KStream#branch
2) org.apache.kafka.streams.kstream.KStream#branch(org.apache.kafka.streams.kstream.Predicate<? super K,? super V>...)
3) org.apache.kafka.streams.kstream.KStream#branch(org.apache.kafka.streams.kstream.Named, org.apache.kafka.streams.kstream.Predicate<? super K,?

**Before/After**
```java
// Before
    /**
     * Creates an array of {@code KStream} from this stream by branching the records in the original stream based on
     * the supplied predicates.
     * Each record is evaluated against the supplied predicates, and predicates are evaluated in order.
     * Each stream in the result array corresponds position-wise (index) to the predicate in the supplied predicates.
     * The branching happens on first-match: A record in the original stream is assigned to the corresponding result
     * stream for the first predicate that evaluates to true, and is assigned to this stream only.
     * A record will be dropped if none of the predicates evaluate to true.
     * This is a stateless record-by-record operation.
     *
     * @param predicates the ordered list of {@link Predicate} instances
     * @return multiple distinct substreams of this {@code KStream}

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12824 remove deprecated method kstream#branch (#16803).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
