# Bug 1881: NullPointerException on listen_address Validation After Bad Merge

**Commit**: aefa20d0fe · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/config/DatabaseDescriptor.java
**Category**: Missing null / bounds check

---

## Root Cause

A merge reordering moved the `listen_address == "0.0.0.0"` rejection check to *after* the `InetAddress.getByName()` call, but outside the `if (conf.listen_address != null)` guard block. As a result, if `conf.listen_address` was `null` the check `conf.listen_address.equals("0.0.0.0")` threw a `NullPointerException` before Cassandra could start. The fix moved the 0.0.0.0 rejection back *inside* the null guard, as the first thing done once a non-null address is confirmed.

### Before / After

```java
// BEFORE — null check is bypassed; 0.0.0.0 check is outside guard
if (conf.listen_address != null)
{
    try { listenAddress = InetAddress.getByName(conf.listen_address); ... }
    catch (...) { throw ...; }
}
if (conf.listen_address.equals("0.0.0.0"))   // NPE when null!
    throw new ConfigurationException("listen_address cannot be 0.0.0.0!");
```

```java
// AFTER — 0.0.0.0 rejection is first statement inside null guard
if (conf.listen_address != null)
{
    if (conf.listen_address.equals("0.0.0.0"))
        throw new ConfigurationException("listen_address cannot be 0.0.0.0!");
    try { listenAddress = InetAddress.getByName(conf.listen_address); ... }
    catch (...) { throw ...; }
}
```

---

## How to Spot

When resolving a merge that moves a block inside or outside a null guard, verify that every access on the guarded variable remains within the guarded scope. Static analysis (e.g., NullAway, FindBugs) and unit tests that start the node with a null `listen_address` would surface this immediately.

---

## Test

No test added.
