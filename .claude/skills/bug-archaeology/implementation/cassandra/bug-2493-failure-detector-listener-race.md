# Bug 2493: ConcurrentModificationException in FailureDetector — listener list iterated while modified

**Commit**: 945c2e24f1 · **Ticket**: CASSANDRA-3519

## Detection Rule

When an `ArrayList` is used for an event listener list that is both iterated (on every detection event) and modified (on register/unregister, which happen on different threads), a `ConcurrentModificationException` can occur during any iteration.

## Code Smell

`private List<IFailureDetectionEventListener> fdEvntListeners_ = new ArrayList<...>()` on a class whose `registerFailureDetectionEventListener()` and `unregisterFailureDetectionEventListener()` are called from different threads than the periodic detection loop that iterates the list.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: arraylist-listener-concurrent-modification
- severity: crash
- subsystem: gossip

## Root Cause

`FailureDetector` stored event listeners in an `ArrayList` and iterated it in `interpret()` (called frequently by gossip) while `register`/`unregister` methods modified it from other threads. The fix replaces `ArrayList` with `CopyOnWriteArrayList`, which provides safe concurrent iteration by making a fresh copy of the underlying array on every write.

## Example

```java
// BEFORE
private List<IFailureDetectionEventListener> fdEvntListeners_ = 
    new ArrayList<IFailureDetectionEventListener>();
```

```java
// AFTER
private List<IFailureDetectionEventListener> fdEvntListeners_ = 
    new CopyOnWriteArrayList<IFailureDetectionEventListener>();
```
