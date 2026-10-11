# Bug 2824: Wrong variable used in local DC write path

**Commit**: 14b7fd1aa8 · **Ticket**: CASSANDRA-1986

## Detection Rule
When a method has two similarly-named local variables (`target` and `destination`) and both are used in adjacent branches, check that each branch uses the correct one.

## Code Smell
A method uses `target` where the loop variable is `destination`, silently sending to the wrong endpoint. The compiler does not complain because both variables have the same type.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: wrong-variable-name
- severity: silent-wrong-result
- subsystem: network-messaging

## Root Cause
After a refactor introduced a `destination` loop variable, one `sendOneWay` call in the local-DC branch was left pointing at `target` (the outer scope's variable) instead of `destination`. Writes to local DC replicas could therefore be sent to the wrong node.

## Example
```java
// BEFORE
MessagingService.instance().sendOneWay(message, target);

// AFTER
MessagingService.instance().sendOneWay(message, destination);
```
