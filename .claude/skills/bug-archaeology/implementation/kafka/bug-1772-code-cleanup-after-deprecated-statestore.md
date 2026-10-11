**Commit**: 8c21857877 · **Ticket**: KAFKA-13722
**Subsystem**: streams/src
**Category**: State not cleaned up

**Root cause**
We remove the deprecated overload of StateStore.init() and thus do not
need to cast any longer. This PR removes all unnecessary casts, and
additionally cleans ups all related classed to reduce warnings. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
        final MockProcessorContext<String, String> context = new MockProcessorContext<String, String>();
        processor.init(new org.apache.kafka.streams.processor.api.MockProcessorContext<String, String>() {
                        public void init(final StateStoreContext context, final StateStore root) {
                            context.register(root, (key, value) -> put(Bytes.wrap(key), value));
                            this.context = context;
            public void init(final StateStoreContext context, final StateStore root) {
                context.register(
     * The implementation of this function must register the root store in the context via the

// After
        final MockProcessorContext<String, String> context = new MockProcessorContext<>();
        processor.init(new org.apache.kafka.streams.processor.api.MockProcessorContext<>() {
                        public void init(final StateStoreContext stateStoreContext, final StateStore root) {
                            stateStoreContext.register(root, (key, value) -> put(Bytes.wrap(key), value));
                            this.context = stateStoreContext;
            public void init(final StateStoreContext stateStoreContext, final StateStore root) {
                stateStoreContext.register(
     * The implementation of this function must register the root store in the stateStoreContext via the
```

**How to spot**
When reviewing streams/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13722: code cleanup after deprecated statestore.init() was removed (#18249).

**Test**
Test modified in WordCountProcessorTest.java covering the corrected behaviour.
