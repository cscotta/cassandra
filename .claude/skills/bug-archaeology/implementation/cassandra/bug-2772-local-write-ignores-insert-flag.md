# Bug 2772: Local Write Inserted Even When insertLocalMessages Is False

**Commit**: 228b65d4e6 · **Ticket**: CASSANDRA-2155

## Detection Rule
When a flag `insertLocalMessages` gates whether local writes should be performed, wrap the `insertLocal` call in a check of that flag rather than calling it unconditionally for the local address.

## Code Smell
`insertLocal(rm, responseHandler)` is called whenever `destination.equals(FBUtilities.getLocalAddress())` with no check of `insertLocalMessages`, bypassing the intent of the flag.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: flag-check-missing
- severity: data-loss
- subsystem: network-messaging

## Root Cause
`StorageProxy.sendMessages` had a branch for the local address that called `insertLocal` unconditionally. The `insertLocalMessages` variable was set based on the write strategy but was never checked before the local insert. This caused a mutation to be applied to the local node even in cases where it should have been skipped (e.g., when the local node's write should only count as a hint or should not be applied). The fix wraps the call: `if (insertLocalMessages) insertLocal(rm, responseHandler)`.

## Example
```java
// BEFORE
if (destination.equals(FBUtilities.getLocalAddress())) {
    insertLocal(rm, responseHandler);
}
```
```java
// AFTER
if (destination.equals(FBUtilities.getLocalAddress())) {
    if (insertLocalMessages)
        insertLocal(rm, responseHandler);
}
```
