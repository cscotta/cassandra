**Commit**: b58aae288f · **Ticket**: KAFKA-19826
**Subsystem**: checkstyle/suppressions.xml
**Category**: State not cleaned up

**Root cause**
Add support for an adaptive batch linger time in the group and share
coordinators. When an adaptive batch linger time is enabled, we no
longer create a timer to flush the current batch. Instead, we append a
flush operation at the end of the event queue so that any currently
queued operations are naturally collected into the batch. To avoid double flushing from hitting the maximum batch size or
transactional writes, we number batches with an epoch to check whether
the batch has already been flushed.

**Before/After**
```java
// Before
        private int appendLingerMs;
        public Builder<S, U> withAppendLingerMs(int appendLingerMs) {
            if (appendLingerMs < 0)
                throw new IllegalArgumentException("AppendLinger must be >= 0");
                if (currentBatch.builder.isTransactional() || (currentTimeMs - currentBatch.appendTimeMs) >= appendLingerMs ||

// After
    <suppress checks="JavaNCSS"
              files="CoordinatorRuntimeTest.java"/>
        private OptionalInt appendLingerMs;
        public Builder<S, U> withAppendLingerMs(OptionalInt appendLingerMs) {
        @SuppressWarnings("checkstyle:CyclomaticComplexity")
            if (appendLingerMs == null)
                appendLingerMs = OptionalInt.empty();
            if (appendLingerMs.isPresent() && appendLingerMs.getAsInt() < 0)
                throw new IllegalArgumentException("AppendLinger must be empty or >= 0");
        /**
         * The batch epoch. Incremented every time a new batch is started.
         * Only valid for the lifetime of the CoordinatorContext. The first batch has an epoch of 1.
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19826: implement coordinator adaptive batch linger time (#20780).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
