**Commit**: 291523e3e4 · **Ticket**: KAFKA-12829
**Subsystem**: streams/processor/api/StateStore
**Category**: Missing override of default method

**Root cause**
`StateStore.init(ProcessorContext, StateStore)` was a deprecated method kept alive as a `default` interface method that delegated to the new `init(StateStoreContext, StateStore)` API. Any store that only implemented the old signature would silently compile and run while using the delegation shim. Removing the deprecated overload and making `init(StateStoreContext, StateStore)` abstract forces all implementations to explicitly implement the current API, eliminating the possibility of a store accidentally using the compatibility bridge at runtime.

**Before/After**
```java
// Before
@Deprecated
default void init(final ProcessorContext context, final StateStore root) {
    if (context instanceof StateStoreContext) {
        init((StateStoreContext) context, root);
    }
}

// After: deprecated overload removed; only abstract method remains
void init(StateStoreContext context, StateStore root);
```

**How to spot**
A `default` method that delegates from a deprecated API to the new one is a compatibility bridge, not a permanent solution. When removing the deprecated API in a major release, also remove the default bridge and make the target method abstract so the compiler enforces migration.

**Test**
All in-tree `StateStore` implementations updated to implement `init(StateStoreContext, StateStore)` directly.
