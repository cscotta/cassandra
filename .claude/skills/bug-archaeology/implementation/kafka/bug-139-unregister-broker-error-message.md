**Commit**: 7bff678699 · **Ticket**: KAFKA-18859
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/admin/KafkaAdminClient.java
**Category**: Wrong serialization / deserialization

**Root cause**
`KafkaAdminClient.unregisterBroker()` constructed error exceptions using `error.exception()` (the static, message-free overload) rather than `error.exception(response.data().errorMessage())`. The server-side `UnregisterBrokerRequest.getErrorResponse()` also failed to populate `errorMessage` in the response data. As a result, detailed server-side error messages were silently discarded and callers received only the generic error name with no actionable detail.

**Before/After**
```java
// BEFORE – drops the server error message
case REQUEST_TIMED_OUT:
    throw error.exception();
default:
    log.error("Unregister broker ... failed: {}", brokerId, error.message());
    future.completeExceptionally(error.exception());

// AFTER – preserves server error message
case REQUEST_TIMED_OUT:
    throw error.exception(response.data().errorMessage());
default:
    log.error("Unregister broker ... failed: {}", brokerId, response.data().errorMessage());
    future.completeExceptionally(error.exception(response.data().errorMessage()));
```

**How to spot**
Search for `error.exception()` calls in response handlers where the response data has an `errorMessage()` field. Any call that ignores the `errorMessage()` field is likely silently discarding the server's human-readable error description. The pattern is especially common in admin-client response parsing code.

**Test**
`testUnregisterBrokerResponseWithUnknownServerError`: builds an `UnregisterBrokerRequest`, calls `getErrorResponse()` with a custom message, and asserts that `response.data().errorMessage()` equals the original exception message.
