**Commit**: 1738c30ab5 · **Ticket**: KAFKA-19863
**Subsystem**: docs/streams
**Category**: Resource leak

**Root cause**
KAFKA-19863: Add documentation on how to implement a custom
exception(with ref to DLQ)

This PR updates the Streams documentation to include guidance on
implementing a custom exception handler with built-in Dead Letter Queue
(DLQ support introduced by KIP-1034. Reviewers Nikita Shuplestov <nshuplestov@confluent.io>, Bill Bejeck
<bbejeck@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
>**Note: The example above demonstrates manual production to a DLQ topic. The following example shows the recommended approach using the built-in DLQ support.**
> A custom processing exception handler can decide whether to continue or fail processing when user logic throws an exception. If DLQ behavior is required, return DLQ records from the handler response.
>
> **Custom Exception Handler Implementation**
>
> The following example forwards failed records to a configured DLQ topic:
>
> ```java
> public class DlqProcessingExceptionHandler implements ProcessingExceptionHandler {
>
>     private String deadLetterQueueTopic;
>
```

**How to spot**
When reviewing docs/streams code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19863: add documentation on how to implement a custom exception(with ref to dlq) (#21606).

**Test**
No test added.
