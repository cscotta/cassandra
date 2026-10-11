# Bug 1982: Missing semicolon in StorageService caused build failure

**Commit**: 373c0336e3 · **Ticket**: n/a
**Subsystem**: service/StorageService.java
**Category**: Logic error in condition

---

## Root Cause

A `throw` statement in `StorageService` was missing its terminating semicolon, causing a compilation error that prevented the project from building. This was a trivial syntax error introduced in a prior commit.

### Before / After

```java
// BEFORE: missing semicolon
throw new IllegalStateException("Unable to contact any seeds!")
```

```java
// AFTER:
throw new IllegalStateException("Unable to contact any seeds!");
```

---

## How to Spot

Build failures from syntax errors are caught immediately by the compiler; the only mitigation is running a build before committing.

---

## Test

No test added.
