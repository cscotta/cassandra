**Commit**: cb9280ecee · **Ticket**: KAFKA-19519
**Subsystem**: coordinator-common/src
**Category**: Logic error in condition

**Root cause**
**Changes**

1. New Dynamic Configurations
- `group.coordinator.cached.buffer.max.bytes`: Largest cached buffer
size
allowed by GroupCoordinator
- `share.coordinator.cached.buffer.max.bytes`: Largest cached buffer
size
allowed by ShareCoordinator

Both configurations default to `1 * 1024 * 1024 + Records.LOG_OVERHEAD`
with minimum value of `512 * 1024`. 2. Extended CoordinatorRuntime Builder Interface

Added withCachedBufferMaxBytesSupplier(Supplier<Integer>
cachedBufferMaxBytesSupplier) method  to allow different coordinator
implementations to supply their buffer  size configuration.

**Before/After**
```java
// Before
import org.apache.kafka.storage.internals.log.LogConfig;
                executorService
        /**
         * The max batch size.
         */
        final int maxBatchSize;

            int maxBatchSize,
            this.maxBatchSize = maxBatchSize;

// After
import java.util.function.Supplier;
        private Supplier<Integer> cachedBufferMaxBytesSupplier;
        public Builder<S, U> withCachedBufferMaxBytesSupplier(Supplier<Integer> cachedBufferMaxBytesSupplier) {
            this.cachedBufferMaxBytesSupplier = cachedBufferMaxBytesSupplier;
            return this;
        }

            if (cachedBufferMaxBytesSupplier == null)
                throw new IllegalArgumentException("Cached buffer max bytes supplier must be set.");
                executorService,
                cachedBufferMaxBytesSupplier
        /**
```

**How to spot**
When reviewing coordinator-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19519 introduce group/share.coordinator.cached.buffer.max.bytes config (#20847).

**Test**
Test modified in CoordinatorRuntimeMetricsImplTest.java covering the corrected behaviour.
