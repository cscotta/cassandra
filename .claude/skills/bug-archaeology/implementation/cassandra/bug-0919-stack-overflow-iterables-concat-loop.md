# Bug 0919: Stack overflow in ModificationStatement::getFunctions due to nested Iterables.concat

**Commit**: 3244774572 · **Ticket**: CASSANDRA-11621
**Subsystem**: CQL / ModificationStatement
**Category**: Resource leak

---

## Root Cause
`ModificationStatement.getFunctions()` accumulated function iterables by chaining `Iterables.concat(existing, next)` inside a loop. Each call wraps the previous `Iterable` in a new `ConcatIterable` without ever materialising the chain. For a statement with many restrictions, operations, or conditions the result is a deeply nested chain of `ConcatIterable` objects. When the returned iterable is eventually traversed, the recursive `hasNext()` / `next()` delegation through dozens or hundreds of nesting levels exhausts the JVM stack, causing a `StackOverflowError`. The fix collects all component iterables into a `LinkedList` first, then calls `Iterables.concat(list)` once at the end, producing a flat single-level concatenation.

### Before / After
```java
// BEFORE – builds O(n) deep nested ConcatIterable chain
Iterable<Function> functions = attrs.getFunctions();
for (Restriction r : processedKeys.values())
    functions = Iterables.concat(functions, r.getFunctions());
for (Operation op : columnOperations)
    functions = Iterables.concat(functions, op.getFunctions());
// ... same for conditions ...
return functions;

// AFTER – flat single concat over a list
List<Iterable<Function>> iterables = new LinkedList<>();
for (Restriction r : processedKeys.values())
    iterables.add(r.getFunctions());
for (Operation op : columnOperations)
    iterables.add(op.getFunctions());
// ... same for conditions ...
return Iterables.concat(iterables);
```

---

## How to Spot
Any accumulation pattern of the form `result = Iterables.concat(result, next)` inside a loop is a stack-depth time bomb. The `concat(a, b)` overload creates a node whose iterator delegates to `a` first then `b`; chaining n times produces n levels of delegation. The safe pattern is to collect all sources into a list and call `Iterables.concat(list)` exactly once outside the loop.

---

## Test
No new test added in this commit.
