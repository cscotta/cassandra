**Commit**: ffcfc974d9 · **Ticket**: KAFKA-19842

**Subsystem**: Kafka Streams / Telemetry integration tests

**Category**: Flaky test

**Root cause**
`KafkaStreamsTelemetryIntegrationTest` was flaky when the `complex + streams` protocol combination was enabled. The new protocol introduced timing-sensitive handshake sequences that the test assertions did not account for, causing intermittent failures unrelated to the telemetry functionality under test. Disabling that specific combination stabilizes the suite while the broader protocol compatibility is addressed separately.

**Before/After**
```java
// Before: all protocol combinations enabled including complex+streams
@ParameterizedTest
@MethodSource("allProtocolCombinations")
void testTelemetry(String protocol) { ... }

// After: complex+streams combination excluded
@ParameterizedTest
@MethodSource("stableProtocolCombinations") // excludes complex+streams
void testTelemetry(String protocol) { ... }
```

**How to spot**
Parameterized integration tests that include in-development protocol combinations often become a source of non-deterministic failures. Isolate unstable parameter sets into a separate, skipped or quarantined test method until the feature is stable.

**Test**
`KafkaStreamsTelemetryIntegrationTest` passes consistently after removing the `complex + streams` parameter combination; the combination is tracked in a separate issue for proper stabilization.
