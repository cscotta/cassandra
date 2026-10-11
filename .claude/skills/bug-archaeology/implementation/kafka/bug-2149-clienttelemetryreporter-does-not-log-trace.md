**Commit**: 24c6e8d085 · **Ticket**: KAFKA-17846
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            final String msg;
            final boolean isTraceEnabled = log.isTraceEnabled();
                    msg = isTraceEnabled ? "" : String.format("the remaining wait time for the %s network API request, as specified by %s", apiName, CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG);
                    msg = isTraceEnabled ? "" : "the terminating push is in progress, disabling telemetry for further requests";
                    msg = isTraceEnabled ? "" : String.format("the client should try to submit the final %s network API request ASAP before closing", ApiKeys.PUSH_TELEMETRY.name);
                        msg = isTraceEnabled ? "" : String.format("the wait time before submitting the next %s network API request has elapsed", apiName);
                        msg = isTraceEnabled ? "" : String.format("the client will wait before submitting the next %s network API request", apiName);

            if (isTraceEnabled) {
                log.trace("For telemetry state {}, returning the value {} ms; {}", localState, timeMs, msg);
            }

// After
                    log.trace("For telemetry state {}, returning the value {} ms; the remaining wait time for the {} network API request, as specified by {}", localState, timeMs, apiName, CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG);
                    log.trace("For telemetry state {}, returning the value {} ms; the terminating push is in progress, disabling telemetry for further requests", localState, timeMs);
                    log.trace("For telemetry state {}, returning the value {} ms; the client should try to submit the final {} network API request ASAP before closing", localState, timeMs, ApiKeys.PUSH_TELEMETRY.name);
                        log.trace("For telemetry state {}, returning the value {} ms; the wait time before submitting the next {} network API request has elapsed", localState, timeMs, apiName);
                        log.trace("For telemetry state {}, returning the value {} ms; the client will wait before submitting the next {} network API request", localState, timeMs, apiName);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17846 clienttelemetryreporter does not log trace-level message (#17570).

**Test**
No test added.
