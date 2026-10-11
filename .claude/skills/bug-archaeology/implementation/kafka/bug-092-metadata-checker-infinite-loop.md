**Commit**: 0ce87e7236 · **Ticket**: KAFKA-18955
**Subsystem**: generator/src/main/java/org/apache/kafka/message/checker/FieldSpecPairIterator.java
**Category**: Logic error in condition

**Root cause**: `FieldSpecPairIterator` uses a state-machine to walk two field lists in parallel. In the `MESSAGE2_ONLY` case (a field exists only in the newer schema), the code validated the field and emitted it, but forgot to advance `iterator1` to its next element: `field1 = iterator1.hasNext() ? iterator1.next() : null;` was missing. Subsequent iterations therefore compared the same `field1` against the advancing `field2`, creating an infinite loop that never terminated. A separate bug in `Unifier` registered both top-level messages against `structRegistry1` instead of registering the second message in `structRegistry2`, causing the evolution verifier to use wrong struct definitions.

**Before/After**

```java
// Before – iterator1 never advanced in MESSAGE2_ONLY branch
case MESSAGE2_ONLY:
    // ...validate...
    break;   // field1 unchanged → infinite loop

// After
case MESSAGE2_ONLY:
    // ...validate...
    field1 = iterator1.hasNext() ? iterator1.next() : null;
    break;
```

**How to spot**: In iterator-based merge loops where only one side advances, always check every `case` to ensure the correct iterator is advanced. A missing advance on one path causes an infinite loop when that path is taken.

**Test**: `MetadataSchemaCheckerToolTest` — exercises the `verify-evolution` and `verify-evolution-git` commands to confirm they terminate and produce the correct output rather than looping indefinitely.
