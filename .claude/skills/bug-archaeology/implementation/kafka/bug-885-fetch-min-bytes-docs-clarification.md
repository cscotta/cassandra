**Commit**: b6fce13e3a · **Ticket**: KAFKA-19460

**Subsystem**: Core / Configuration documentation

**Category**: Misleading/wrong documentation or log message

**Root cause**
The documentation for `fetch.min.bytes` and `replica.fetch.min.bytes` stated that the broker waits until at least the configured number of bytes are available before responding. It did not clarify that per-partition size limits can cause the actual response to contain less data than the configured minimum, leading operators to incorrectly diagnose "under-fetching" as a misconfiguration rather than an expected behavior of the per-partition cap.

**Before/After**
```
// Before: docs imply response will always contain >= fetch.min.bytes
fetch.min.bytes: The minimum amount of data the server should return
for a fetch request.

// After: clarification added
fetch.min.bytes: The minimum amount of data the server should return
for a fetch request. Note: the actual data returned may be less due to
per-partition fetch limits.
```

**How to spot**
When config documentation describes a minimum or guarantee that is subject to per-partition overrides, always document the exception condition explicitly to avoid misleading users into misconfiguring their systems.

**Test**
Documentation review confirms that both `fetch.min.bytes` and `replica.fetch.min.bytes` entries now include the per-partition-limit caveat.
