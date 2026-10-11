**Commit**: a51eec17e1 · **Ticket**: KAFKA-17765
**Subsystem**: test utilities
**Category**: State not cleaned up

**Root cause**
`TestUtils.toSet(list)` was a bespoke utility wrapper around `new HashSet<>(list)` that accumulated as dead code once Java 10 introduced `Set.copyOf(list)`. Keeping a custom wrapper obscures intent, cannot be inlined by IDEs, and adds an unnecessary maintenance surface for a single-line operation available in the standard library.

**Before/After**
```java
// Before
Set<String> s = TestUtils.toSet(myList);

// After
Set<String> s = Set.copyOf(myList);
```

**How to spot**
When reviewing test utility classes, compare each method to its Java standard-library equivalent. Methods that simply delegate to `new HashSet<>(c)`, `new ArrayList<>(c)`, etc. should be removed in favour of `Set.copyOf` / `List.copyOf` introduced in Java 10.

**Test**
No test added.
