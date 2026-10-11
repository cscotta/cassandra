**Commit**: b03fe66cfe · **Ticket**: KAFKA-17759
**Subsystem**: common (Utils.mkSet)
**Category**: State not cleaned up

**Root cause**
`Utils.mkSet(T... elems)` was a varargs helper that wrapped `new HashSet<>(Arrays.asList(elems))`. Java 9 introduced `Set.of(...)` which is shorter, produces an immutable set, and is understood by every IDE. The custom helper was dead value: it added an import, imposed a mutable `HashSet` when immutability was preferred, and obscured intent across hundreds of call sites.

**Before/After**
```java
// Before
Set<String> s = Utils.mkSet("a", "b", "c");

// After
Set<String> s = Set.of("a", "b", "c");
```

**How to spot**
Search for `Utils.mkSet`, `Utils.mkList`, and similar collection factory wrappers; replace them with `Set.of` / `List.of` and remove the helper once all callers are migrated. Note: `Set.of` disallows nulls, so verify no existing call passes null before replacing.

**Test**
No test added.
