**Commit**: 962f4ada75 · **Ticket**: KAFKA-19203
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/admin/KafkaAdminClient.java
**Category**: Wrong serialization / deserialization

**Root cause**
Multiple sites in `KafkaAdminClient` constructed an `ApiError` object solely to call `.exception()` on it, which wrapped the real exception in an unnecessary intermediate layer. The correct approach is to call `error.exception(message)` directly on the `Errors` enum value, which constructs the exception with the original error message from the response. Using `new ApiError(error, message).exception()` could produce a differently typed or differently messaged exception depending on `ApiError`'s internal wrapping logic, diverging from what `Errors.exception(String)` returns directly.

**Before/After**
```java
// Before: unnecessary ApiError wrapper
ApiError apiError = new ApiError(error, response.data().errorMessage());
handleFailure(apiError.exception());

// After: direct exception construction from Errors
handleFailure(error.exception(response.data().errorMessage()));
```

**How to spot**
Search for `new ApiError(...)` followed immediately by `.exception()` — this pattern is always redundant since `Errors` already has an `exception(String)` factory method that does the same thing more directly without extra allocation or potential type mismatch.

**Test**
No new test added. The change is a refactoring that removes redundant wrapping; existing admin client tests cover the affected code paths.
