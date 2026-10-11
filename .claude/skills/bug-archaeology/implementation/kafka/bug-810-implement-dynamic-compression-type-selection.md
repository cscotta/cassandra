**Commit**: b4bf0bf693 · **Ticket**: KAFKA-19506
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
#### Summary
This PR implements dynamic compression type selection and fallback
mechanism for client telemetry to handle cases where compression
libraries are not available on the client classpath. #### Problem
Currently, when a compression library is missing (e.g.,
NoClassDefFoundError), the client telemetry system catches the generic
Throwable but doesn't learn from the failure. This means, the same
unsupported compression type will be attempted on every telemetry push

#### Solution
This PR introduces a comprehensive fallback mechanism:
- Specific Exception Handling: Replace generic Throwable catching with
specific exceptions (IOException, NoClassDefFoundError)
- Unsupported Compression T...

**Before/After**
```java
// Before
// (no removals)

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19506 implement dynamic compression type selection and fallback for client telemetry (#20144).

**Test**
Test modified in ClientTelemetryReporterTest.java covering the corrected behaviour.
