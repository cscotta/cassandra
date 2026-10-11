**Commit**: 9c9f1446a1 · **Ticket**: KAFKA-18061
**Subsystem**: raft / RaftUtil
**Category**: Wrong constant / default value

**Root cause**
`RaftUtil.addVoterResponse` and `RaftUtil.removeVoterResponse` used `errorMessage = errorMessage == null ? error.message() : errorMessage` unconditionally. For the success case (`Errors.NONE`), `Errors.NONE.message()` returns the string `"NONE"` rather than `null`. Clients that inspect `errorMessage` to determine whether an error occurred therefore saw the non-null string `"NONE"` on a successful response, introducing a semantic inconsistency.

**Before/After**
```java
// Before — "NONE" string used for successful responses
errorMessage = errorMessage == null ? error.message() : errorMessage;

// After — null preserved for NONE, message used only for actual errors
if (errorMessage == null && error != Errors.NONE) {
    errorMessage = error.message();
}
```

**How to spot**
Any code that falls back to `error.message()` when `errorMessage` is null must special-case `Errors.NONE`, because `Errors.NONE.message()` returns a non-null sentinel string ("NONE") that looks like an error to callers.

**Test**
`RaftUtilTest.testAddVoterResponse` and `testRemoveVoterResponse` — iterate over all `Errors` values, call the helper, and assert: for `NONE` the `errorMessage` field is null; for all others it equals `error.message()`.
