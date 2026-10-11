**Commit**: 66cc6b6297 · **Ticket**: KAFKA-19939
**Subsystem**: docs/streams
**Category**: Resource leak

**Root cause**
Implements KIP-1270. Extends the ProcessingExceptionHandler to support global  statestore
processing in addition to the existing stream task  processing. Previously, the handler only worked for regular stream  tasks; now it
also covers exceptions during global state restoration and  runtime
processing. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>, Matthias J.

**Before/After**
```java
// Before
> **Note:** This handler applies only to regular stream processing tasks. It does not apply to global state store updates (global threads). Exceptions occurring in global threads will bubble up to the configured uncaught exception handler.

// After
    * processing.exception.handler.global.enabled (deprecated)
> **Note:** By default, this handler applies only to regular stream processing tasks. To enable exception handling for global stores/KTable processing (which is recommended), see `processing.exception.handler.global.enabled` below. When global exception handling is disabled (default), exceptions occurring during global store/KTable processing will bubble up to the configured uncaught exception handler.
### processing.exception.handler.global.enabled (deprecated)

> Controls whether the configured `ProcessingExceptionHandler` is invoked for exceptions occurring during global store/KTable processing. When set to `true` (recommended), the handler specified via `processing.exception.handler` will be invoked for exceptions occurring during global store/KTable processing. When set to `false` (default), exceptions from global store/KTable will not invoke the processing exception handler and will instead bubble up to the configured uncaught exception handler.
>
> **Default value:** `false`
>
> **Deprecated:** The config is deprecated for removal in 5.0 release. With the removal of the config, the processing exception handler will be applied during global state/KTable processing and cannot be disabled any longer. Thus, it's recommended to enable this config now, to avoid backward incompatibilities in the future.
>
> **Important Notes:**
>
```

**How to spot**
When reviewing docs/streams code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19939 extend processingexceptionhandler to globalktable processors (#21535).

**Test**
Test modified in GlobalKTableIntegrationTest.java covering the corrected behaviour.
