# Bug 1869: Fix AbstractRowResolver and RowDigestResolver for speculative retry

**Commit**: 01370bb6c7 · **Ticket**: CASSANDRA-6194
**Subsystem**: service (read path / speculative retry)
**Category**: Logic error in condition

---

## Root Cause

With speculative retry, two full-data responses can legitimately arrive for the same read: one
from the original target and one from the speculative replica.  `AbstractRowResolver.preprocess()`
tried to de-duplicate by replacing an existing reply from the same host, but this logic was
incorrect — it suppressed the count increment in `ReadCallback.response()` only when a
replacement happened, not when two *different* hosts both returned data.  Meanwhile
`RowDigestResolver.resolve()` assumed at most one data reply and compared all others as digest
replies against a single digest, so a second data response would not be hashed correctly and
could cause a false `DigestMismatchException` or return the wrong row.  The fix removes the
de-duplication logic entirely, always appends replies, and rewrites `RowDigestResolver` to
compute a digest uniformly for both data and digest responses.

### Before / After

```java
// BEFORE – AbstractRowResolver tried to replace duplicate-host replies and
//           returned false to suppress the received counter increment
public boolean preprocess(MessageIn<ReadResponse> message) {
    // ... complex replace-if-same-host logic ...
    replies.add(message);
    return true;
}
// ReadCallback:
boolean hasAdded = resolver.preprocess(message);
int n = (waitingFor(message) && hasAdded) ? received.incrementAndGet() : received.get();
```

```java
// AFTER – always append; counter always incremented when waitingFor()
public void preprocess(MessageIn<ReadResponse> message) {
    replies.add(message);
}
// ReadCallback:
resolver.preprocess(message);
int n = waitingFor(message) ? received.incrementAndGet() : received.get();
```

```java
// BEFORE – RowDigestResolver treated data and digest responses differently,
//           assuming only one data reply
if (response.isDigestQuery()) {
    // accumulate digest
} else {
    data = response.row().cf;  // last data reply wins, earlier ones ignored
}

// AFTER – compute digest uniformly from both types
newDigest = response.isDigestQuery() ? response.digest() : ColumnFamily.digest(data);
if (digest == null) digest = newDigest;
else if (!digest.equals(newDigest)) throw new DigestMismatchException(...);
```

---

## How to Spot

When a response-accumulation path adds de-duplication or conditional counting logic, verify that
the logic remains correct under all valid delivery scenarios — including speculative paths that
intentionally send to more replicas than the consistency level requires.  A suppressed counter
increment that should not be suppressed can cause reads to block until timeout.

---

## Test

No test added.
