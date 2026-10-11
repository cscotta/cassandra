# Bug 1556: Fix cqlsh Authentication with 2.1 Python Driver

**Commit**: e4b40c75c0 · **Ticket**: CASSANDRA-7564
**Subsystem**: bin/cqlsh
**Category**: Wrong constant / default value

---

## Root Cause

The cqlsh script constructed the `auth_provider` as a plain lambda returning a dict (`lambda host: dict(username=..., password=...)`). This worked with the older cassandra-driver 2.0 API, which accepted a callable returning a dict of credentials. The 2.1 python driver dropped support for that callable form and instead requires a proper `AuthProvider` object. The fix replaces the lambda with an instantiation of `PlainTextAuthProvider`, which is the correct API for the 2.1+ driver.

### Before / After

```python
# BEFORE
self.auth_provider = lambda host: dict(username=username, password=password)
```

```python
# AFTER
from cassandra.auth import PlainTextAuthProvider
self.auth_provider = PlainTextAuthProvider(username=username, password=password)
```

---

## How to Spot

When a library's major version changes, check all integration points that rely on callable or duck-typed contracts — these are not caught by static analysis. The pattern `lambda ...: dict(...)` as an auth callback is a red flag when the dependency has been bumped.

---

## Test

No test added.
