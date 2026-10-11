# Bug 2968: Collection.toArray() returns Object[] causing ClassCastException at cast site

**Commit**: d748648b23 · **Ticket**: CASSANDRA-338

## Detection Rule
When casting the result of `collection.toArray()` to a typed array (e.g., `(Foo[]) list.toArray()`), flag it — `toArray()` with no argument always returns `Object[]` and the cast will throw `ClassCastException` at runtime.

## Code Smell
`(StreamContextManager.StreamContext[]) streamContexts.toArray()` calls the no-arg `toArray()` which returns `Object[]`; the explicit cast to `StreamContext[]` compiles but throws `ClassCastException` at runtime.

## Tags
- scope: java
- category: wrong-serialization
- code-pattern: untyped-toArray-cast
- severity: crash
- subsystem: streaming

## Root Cause
`Collection.toArray()` (no-arg) always returns `Object[]` regardless of the collection's generic type parameter. Casting to `StreamContext[]` produces a `ClassCastException` when the JVM checks the actual array type. The correct idiom is `collection.toArray(new T[collection.size()])` which allocates a properly typed array and fills it. This bug affected both the `StreamManager.addFilesToStream()` and `BootstrapInitiateMessage` constructor calls.

## Example
```java
// BEFORE: ClassCastException at runtime
StreamManager.instance(target).addFilesToStream(
    (StreamContextManager.StreamContext[]) streamContexts.toArray());
```
```java
// AFTER: typed toArray with pre-allocated array
final StreamContextManager.StreamContext[] contexts =
    streamContexts.toArray(new StreamContextManager.StreamContext[streamContexts.size()]);
StreamManager.instance(target).addFilesToStream(contexts);
```
