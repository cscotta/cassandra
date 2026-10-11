# Bug 1253: cassandra-stress Unconditionally Connects to Thrift Even in Native-Only Mode

**Commit**: d26bfe9a4d · **Ticket**: n/a
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/StressProfile.java
**Category**: Logic error in condition

---

## Root Cause

`StressProfile` called `settings.getThriftClient()` and `tclient.prepare_cql3_query()` unconditionally during profile initialization, even when the connection mode was `JAVA_DRIVER_NATIVE`. This forced a Thrift connection to be established (and a Thrift-side prepared statement to be created) for every run, regardless of whether Thrift was needed. On clusters where Thrift was disabled or unavailable, this caused the tool to fail at startup. The fix gates both the Thrift client acquisition and the Thrift `prepare_cql3_query` call behind a check for `settings.mode.api != ConnectionAPI.JAVA_DRIVER_NATIVE`.

### Before / After

```java
// BEFORE – Thrift always acquired
JavaDriverClient jclient = settings.getJavaDriverClient();
ThriftClient tclient = settings.getThriftClient();   // always called
...
tids.put(key, tclient.prepare_cql3_query(cql, Compression.NONE));  // always called
```

```java
// AFTER – Thrift acquired only when needed
ThriftClient tclient = null;
if (settings.mode.api != ConnectionAPI.JAVA_DRIVER_NATIVE)
    tclient = settings.getThriftClient();
...
if (tclient != null)
    tids.put(key, tclient.prepare_cql3_query(cql, Compression.NONE));
```

---

## How to Spot

When a code path acquires an external connection or resource, look for whether there is a mode or configuration flag that makes that resource unnecessary. Unconditional acquisition before a branch that checks necessity is a common pattern leading to spurious failures on environments where the unused protocol is disabled.

---

## Test

No test added.
