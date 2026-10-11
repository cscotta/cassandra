# Bug 2723: Row-cache provider stored as raw class name, recreated on every getRowCacheProvider call

**Commit**: 16651d9ca8 · **Ticket**: n/a

## Detection Rule
When a field is logically an object instance, store the instance rather than a string name and instantiate once, not on every accessor call.

## Code Smell
`CFMetaData.rowCacheProvider` is typed as `String`; `getRowCacheProvider()` calls `FBUtilities.newCacheProvider(rowCacheProvider)` each time, which reflectively instantiates a new object per invocation.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: repeated-reflective-instantiation
- severity: performance
- subsystem: storage-engine

## Root Cause
`rowCacheProvider` was stored as a class-name `String`. Every call to `getRowCacheProvider()` invoked `FBUtilities.newCacheProvider()`, which uses reflection to create a new instance. This means every read path that checked the cache provider allocated a fresh object. The fix changes the field type to `IRowCacheProvider`, stores the instance at construction/update time, and makes the accessor a simple field return.

## Example
```java
// BEFORE — new instance created on every call
private String rowCacheProvider;

public IRowCacheProvider getRowCacheProvider() {
    return FBUtilities.newCacheProvider(rowCacheProvider);
}
```
```java
// AFTER — instance stored once, accessor is O(1)
private IRowCacheProvider rowCacheProvider;

public IRowCacheProvider getRowCacheProvider() {
    return rowCacheProvider;
}
```
