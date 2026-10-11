**Commit**: 6a4207f12a · **Ticket**: KAFKA-19158
**Subsystem**: clients/src/main/java/org/apache/kafka/common/protocol/Errors.java
**Category**: Wrong constant / default value

**Root cause**
There was no protocol error code defined for the case where a broker has reached its maximum number of concurrent share sessions. Without a dedicated error code, clients could not distinguish this condition from other errors and could not implement appropriate retry or backoff behaviour. The fix adds error code 133 (`SHARE_SESSION_LIMIT_REACHED`) mapped to a new `ShareSessionLimitReachedException` that extends `RetriableException`, correctly signalling that the condition is transient and the client should retry.

**Before/After**
```java
// Before: no error code for share session limit
STREAMS_TOPOLOGY_FENCED(132, "The supplied topology epoch is outdated.", StreamsTopologyFencedException::new);

// After: new retriable error code added
STREAMS_TOPOLOGY_FENCED(132, "The supplied topology epoch is outdated.", StreamsTopologyFencedException::new),
SHARE_SESSION_LIMIT_REACHED(133, "The limit of share sessions has been reached.", ShareSessionLimitReachedException::new);
```

**How to spot**
When a new server-side resource limit is introduced, always check that a corresponding protocol error code exists. If the server can only respond with a generic error (like `UNKNOWN_SERVER_ERROR`), the client cannot distinguish the condition and will not implement correct retry semantics.

**Test**
No test added in this commit; the change is a protocol enumeration addition. Tests for client and broker behaviour using this error code were noted to follow in subsequent PRs.
