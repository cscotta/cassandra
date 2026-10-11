# Bug 0192: Static Final Field Read at Class-Load Time Defeats Runtime Configuration

**Commit**: c4d11c4372 · **Ticket**: CASSANDRA-18715
**Subsystem**: index/sai/plan/QueryController
**Category**: Wrong constant / default value

---

## Root Cause

`QueryController.ORDER_CHUNK_SIZE` was a `public static final int` initialised once at class-load time by calling `SAI_VECTOR_SEARCH_ORDER_CHUNK_SIZE.getInt()`. Any subsequent change to the underlying system property (e.g. via `setInt()` in tests or via dynamic reconfiguration) had no effect on queries because the value was permanently baked into the static field. On JDK 17, strong encapsulation also prevented the reflection-based workaround the test used to overwrite the final field, making the test fail outright.

### Before / After

```java
// BEFORE
public static final int ORDER_CHUNK_SIZE = SAI_VECTOR_SEARCH_ORDER_CHUNK_SIZE.getInt();
// used as:
new KeyRangeOrderingIterator(source, ORDER_CHUNK_SIZE, ...)
```

```java
// AFTER
private final int orderChunkSize;
// in constructor:
this.orderChunkSize = SAI_VECTOR_SEARCH_ORDER_CHUNK_SIZE.getInt();
// used as:
new KeyRangeOrderingIterator(source, orderChunkSize, ...)
```

---

## How to Spot

When a configuration value drives behaviour that must be settable at runtime or per-instance, storing it in a `static final` primitive field initialised at class-load time is a bug: Java resolves final static primitives at compile/link time in callers, so no amount of reflection or property mutation will change what the executing code sees. Any field whose value should vary between test invocations or be overridable by a system property must be read lazily (e.g. in a constructor, or by calling the accessor at each use site).

---

## Test

No new test added; the existing `VectorUpdateDeleteTest.testChunkSizes` was refactored to call `SAI_VECTOR_SEARCH_ORDER_CHUNK_SIZE.setInt()` directly instead of the broken reflection helper, and now passes on JDK 17.
