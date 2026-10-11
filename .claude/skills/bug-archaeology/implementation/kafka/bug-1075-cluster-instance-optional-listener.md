## Commit

7d4acedc27 — KAFKA-19270: ClusterInstance.controllerListenerName returns Optional, forcing callers to unwrap

## Ticket

KAFKA-19270

## Subsystem

Test infrastructure / ClusterInstance API

## Category

Missing null / bounds check

## Root cause

`ClusterInstance.controllerListenerName()` returned `Optional<ListenerName>` with a default implementation that returned `Optional.empty()`. Every call site had to call `.orElseThrow()` or `.get()` without a guard, meaning that any cluster type that forgot to override the method would silently return empty and cause a `NoSuchElementException` at the unwrap site rather than at the type that should have provided the value. The fix changes the return type to `ListenerName` (non-optional), causing a compile error in any implementation that does not override the method.

## Before / After

```java
// Before — Optional with silent empty default, unwrap errors at call site
default Optional<ListenerName> controllerListenerName() {
    return Optional.empty();
}
// Call site:
ListenerName name = cluster.controllerListenerName().orElseThrow();

// After — non-optional, compile error if not overridden
ListenerName controllerListenerName();
// Call site:
ListenerName name = cluster.controllerListenerName();
```

## How to spot

Search for `default` methods in test interface abstractions that return `Optional.empty()`. These silent defaults push the error to the call site, where the optional is unwrapped without context. Returning the value directly (and making the method abstract) forces each implementation to provide a real value.

## Test

`RaftClusterInvocationContext` was updated to implement the new non-optional signature; all existing tests using `controllerListenerName()` compile and pass without `orElseThrow()`.
