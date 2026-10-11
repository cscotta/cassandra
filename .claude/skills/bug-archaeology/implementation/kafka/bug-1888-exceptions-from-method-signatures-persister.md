**Commit**: 22f4c4a173 · **Ticket**: KAFKA-18148
**Subsystem**: share/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Abhinav Dixit <adixit@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public CompletableFuture<InitializeShareGroupStateResult> initializeState(InitializeShareGroupStateParameters request) throws IllegalArgumentException {
    public CompletableFuture<DeleteShareGroupStateResult> deleteState(DeleteShareGroupStateParameters request) throws IllegalArgumentException {
    public CompletableFuture<ReadShareGroupStateSummaryResult> readSummary(ReadShareGroupStateSummaryParameters request) throws IllegalArgumentException {
    CompletableFuture<InitializeShareGroupStateResult> initializeState(InitializeShareGroupStateParameters request) throws IllegalArgumentException;
    CompletableFuture<DeleteShareGroupStateResult> deleteState(DeleteShareGroupStateParameters request) throws IllegalArgumentException;
    CompletableFuture<ReadShareGroupStateSummaryResult> readSummary(ReadShareGroupStateSummaryParameters request) throws IllegalArgumentException;

// After
    public CompletableFuture<InitializeShareGroupStateResult> initializeState(InitializeShareGroupStateParameters request) {
    public CompletableFuture<DeleteShareGroupStateResult> deleteState(DeleteShareGroupStateParameters request) {
    public CompletableFuture<ReadShareGroupStateSummaryResult> readSummary(ReadShareGroupStateSummaryParameters request) {
    CompletableFuture<InitializeShareGroupStateResult> initializeState(InitializeShareGroupStateParameters request);
    CompletableFuture<DeleteShareGroupStateResult> deleteState(DeleteShareGroupStateParameters request);
    CompletableFuture<ReadShareGroupStateSummaryResult> readSummary(ReadShareGroupStateSummaryParameters request);
```

**How to spot**
When reviewing share/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18148 remove exceptions from method signatures in persister (#18064).

**Test**
No test added.
