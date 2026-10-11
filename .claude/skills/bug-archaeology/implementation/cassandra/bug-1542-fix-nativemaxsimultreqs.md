# Bug 1542: Fix nativeMaxSimultReqs Not Applied in CqlNativeStorage

**Commit**: 3744d77929 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hadoop/pig
**Category**: Silent skip on non-match

---

## Root Cause

`CqlNativeStorage.setLocation()` correctly parsed the `nativeMaxSimultReqs` configuration option and stored it in a field, but never forwarded it to `CqlConfigHelper.setInputMaxSimultReqPerConnections()`. The `nativeMinSimultReqs` counterpart was properly applied one line above. As a result, users who set `nativeMaxSimultReqs` in their Pig scripts would see it silently ignored, with the default maximum simultaneous requests per connection always being used.

### Before / After

```java
// BEFORE — nativeMaxSimultReqs is never forwarded
if (nativeMinSimultReqs != null)
    CqlConfigHelper.setInputMinSimultReqPerConnections(conf, nativeMinSimultReqs);
// nativeMaxSimultReqs silently dropped
if (nativeConnectionTimeout != null)
    CqlConfigHelper.setInputNativeConnectionTimeout(conf, nativeConnectionTimeout);
```

```java
// AFTER
if (nativeMinSimultReqs != null)
    CqlConfigHelper.setInputMinSimultReqPerConnections(conf, nativeMinSimultReqs);
if (nativeMaxSimultReqs != null)
    CqlConfigHelper.setInputMaxSimultReqPerConnections(conf, nativeMaxSimultReqs);
if (nativeConnectionTimeout != null)
    CqlConfigHelper.setInputNativeConnectionTimeout(conf, nativeConnectionTimeout);
```

---

## How to Spot

When a class has parallel min/max (or similar paired) configuration fields, verify that both are forwarded wherever one is. A lone `nativeMin*` application with no corresponding `nativeMax*` in the same block is a strong signal that the max case was forgotten.

---

## Test

No test added.
