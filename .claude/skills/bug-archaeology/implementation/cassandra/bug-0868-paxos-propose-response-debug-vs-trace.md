# Bug 0868: Paxos Propose Response Logged at DEBUG Instead of TRACE

**Commit**: 11b93152df · **Ticket**: CASSANDRA-12155
**Subsystem**: Paxos / ProposeCallback
**Category**: Wrong constant / default value

---

## Root Cause
`ProposeCallback.response` logged each incoming proposal response at `DEBUG` level. Under any non-trivial write workload, every write that goes through Paxos generates at least one propose round, producing one log message per replica response. With DEBUG logging enabled (a common scenario during troubleshooting), this floods the log with low-value per-response entries and obscures other diagnostics. The correct granularity for per-message tracing is `TRACE`, which must be explicitly enabled and is understood to be high-volume.

### Before / After
```java
// BEFORE
logger.debug("Propose response {} from {}", msg.payload, msg.from);

// AFTER
logger.trace("Propose response {} from {}", msg.payload, msg.from);
```

---

## How to Spot
In hot code paths (message callbacks, per-partition or per-row loops, per-request handlers), `logger.debug` is appropriate only for coarse-grained events fired at most once per logical operation. Any per-response, per-row, or per-iteration log call at DEBUG or above should be reviewed to determine whether it should be TRACE. The symptom is log flooding that makes DEBUG mode unusable during load testing or incident investigation.

---

## Test
No test added.
