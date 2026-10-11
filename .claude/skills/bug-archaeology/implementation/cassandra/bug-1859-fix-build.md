# Bug 1859: Hadoop InputFormat openTransport Call Passes Removed conf Argument

**Commit**: dc71d22003 · **Ticket**: n/a
**Subsystem**: hadoop (AbstractColumnFamilyInputFormat)
**Category**: Logic error in condition

---

## Root Cause

After the `ITransportFactory.openTransport` API was refactored to remove the `conf` parameter, the Hadoop `AbstractColumnFamilyInputFormat` was not updated in tandem. The call site continued passing the `conf` argument, causing a compilation failure because no matching method signature existed any longer.

### Before / After

```java
// BEFORE
transport = ConfigHelper.getClientTransportFactory(conf).openTransport(location, port, conf);
```

```java
// AFTER
transport = ConfigHelper.getClientTransportFactory(conf).openTransport(location, port);
```

---

## How to Spot

When refactoring a method signature, search all call sites across the entire codebase before committing — in particular in sub-modules (hadoop, tools) that may not be part of the default build target. A compile-all step or CI matrix covering optional modules would have caught this immediately.

---

## Test

No test added.
