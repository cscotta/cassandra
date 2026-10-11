**Commit**: daece61a50 · **Ticket**: MINOR

**Subsystem**: Core utilities / LockUtils / AbstractIndex

**Category**: Refactoring correctness (API rename, overload)

**Root cause**
`LockUtils.inLockThrows` was the only variant accepting a throwing supplier, but its name was inconsistent with the non-throwing `inLock`. The refactor renames `inLockThrows` to `inLock` as an overload accepting `ThrowingSupplier` or `ThrowingRunnable`, and removes the separate `inLockThrows` entry point. `AbstractIndex` methods were updated to call the renamed overload. Any code still calling `inLockThrows` would fail to compile, making the rename a safe, compile-time-enforced API cleanup.

**Before/After**
```java
// Before: separate method name for throwing variant
LockUtils.inLockThrows(lock, () -> { ... throw new IOException(); ... });

// After: unified overloaded name
LockUtils.inLock(lock, () -> { ... throw new IOException(); ... });
```

**How to spot**
Search for `inLockThrows` across the codebase; any remaining reference after this commit indicates a call site that was not updated. The compiler will catch these during build.

**Test**
`LockUtilsTest` covers both throwing and non-throwing overloads of `inLock` to confirm correct exception propagation and lock release in all branches.
