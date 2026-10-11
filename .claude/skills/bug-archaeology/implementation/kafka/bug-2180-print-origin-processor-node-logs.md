**Commit**: d6b5943570 · **Ticket**: KAFKA-17099
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR leverages the updates brought by KIP-1033 to get the name of the processor node which raised a processing exception and display it in the stacktrace instead of the source node. Reviewer: Bruno Cadonna <cadonna@apache.org>

**Before/After**
```java
// Before
    public FailedProcessingException(final String errorMessage, final Exception exception) {
    public FailedProcessingException(final Exception exception) {
                throw new FailedProcessingException("Fatal user code error in processing error callback", fatalUserException);
                throw new FailedProcessingException(processingException);
            throw new FailedProcessingException("Fatal user code error in production error callback", fatalUserException);
                sendException.set(new FailedProcessingException("Fatal user code error in production error callback", fatalUserException));
            handleExcept

// After
    private final String failedProcessorNodeName;
    public FailedProcessingException(final String errorMessage, final String failedProcessorNodeName, final Exception exception) {
        this.failedProcessorNodeName = failedProcessorNodeName;
    public FailedProcessingException(final String failedProcessorNodeName, final Exception exception) {
        this.failedProcessorNodeName = failedProcessorNodeName;
    }

    public String failedProcessorNodeName() {
        return failedProcessorNodeName;
                throw new FailedProcessingException(
                    "Fatal user code error in processing error callback",
                    internalProcessorContext.currentNode().name(),
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17099: print the origin processor node in logs when a processing exception occurs (#17475).

**Test**
Test modified in ProcessingExceptionHandlerIntegrationTest.java covering the corrected behaviour.
