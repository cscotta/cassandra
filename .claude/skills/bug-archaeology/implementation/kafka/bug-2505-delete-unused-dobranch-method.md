**Commit**: 3d436f52bf · **Ticket**: KAFKA-12824
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.lang.reflect.Array;
    private static final String BRANCH_NAME = "KSTREAM-BRANCH-";

    private static final String BRANCHCHILD_NAME = "KSTREAM-BRANCHCHILD-";

    @SuppressWarnings({"unchecked", "rawtypes"})
    private KStream<K, V>[] doBranch(final NamedInternal named,
                                     final Predicate<? super K, ? super V>... predicates) {
        Objects.requireNonNull(predicates, "predicates can't be a null array");
        if (predicates.length == 0) {
            throw new IllegalArgumentException("branch() requires at least one predicate");
        }

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12824 delete unused dobranch method (#16981).

**Test**
No test added.
