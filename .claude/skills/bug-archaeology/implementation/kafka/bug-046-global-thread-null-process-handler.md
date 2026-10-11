**Commit**: 7cea0595f7 · **Ticket**: KAFKA-19930
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/processor/internals/ProcessorNode.java
**Category**: Missing null / bounds check

**Root cause**
`ProcessorNode.process` invoked the `processingExceptionHandler` to handle runtime exceptions without first checking whether the handler was set. Global state-store threads initialize `ProcessorNode` without a `ProcessingExceptionHandler` (it is only set for regular stream tasks), so a processing exception in a global thread caused a `NullPointerException` on the handler, masking the original exception and preventing the uncaught exception handler from receiving meaningful information.

**Before/After**
```java
// Before – NullPointerException if handler is null (global thread case)
final ErrorHandlerContext errorHandlerContext = new DefaultErrorHandlerContext(...);
final ProcessingExceptionHandler.ProcessingHandlerResponse response =
    processingExceptionHandler.handle(errorHandlerContext, ...);

// After – rethrow immediately when no handler is configured
if (processingExceptionHandler == null) {
    throw processingException;
}
// ... then delegate to handler as before
```

**How to spot**
Any optional collaborator that is set only for specific configurations (here: stream tasks but not global tasks) must be null-checked before use. A comment or guard at the initialization site should document which configurations leave the field null.

**Test**
`ProcessorNodeTest#shouldRethrowExceptionWhenProcessingExceptionHandlerIsNull`: creates a `ProcessorNode` without calling the method that sets the handler (simulating global-thread initialization), invokes `process`, and asserts that the original `RuntimeException` is rethrown directly.
