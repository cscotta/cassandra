# Bug 1146: Wrong Verb Used for Batchlog Mutations Sent to Pre-3.0 Nodes

**Commit**: 16498de42d · **Ticket**: CASSANDRA-9992
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Wrong constant / default value

---

## Root Cause

`StorageProxy` introduced `Verb.BATCHLOG_MUTATION` for 3.0+ nodes, but the version-dispatch logic was inverted: the old-style `Verb.MUTATION` message was sent to current-version peers (which expect `BATCHLOG_MUTATION`), while the new `BATCHLOG_MUTATION` was sent to older peers (which do not understand it). Additionally, the second batchlog-write path (`writeToBatchlog`) did not check the target version at all before sending, so it always used the 3.0 verb regardless of whether the remote node was running an older protocol. Both paths needed the same fix: send `Verb.MUTATION` to pre-3.0 nodes and `Verb.BATCHLOG_MUTATION` to 3.0+ nodes.

### Before / After

```java
// BEFORE – condition is backwards; old nodes get the new verb
else if (targetVersion == MessagingService.current_version)
    MessagingService.instance().sendRR(message, target, handler, false);
else
    MessagingService.instance().sendRR(getBatchlogMutationFor(...)
                                        .createMessage(Verb.BATCHLOG_MUTATION), target, handler, false);
```

```java
// AFTER – old nodes get Verb.MUTATION; 3.0+ nodes get Verb.BATCHLOG_MUTATION
else if (targetVersion < MessagingService.VERSION_30)
    MessagingService.instance().sendRR(getBatchlogMutationFor(...)
                                        .createMessage(Verb.MUTATION), target, handler, false);
else
    MessagingService.instance().sendRR(message, target, handler, false);
```

---

## How to Spot

Whenever a version-gated branch uses `== current_version` (equality) rather than `< VERSION_X` (comparison), the logic almost certainly mishandles both the mixed-version upgrade path and any future version. Reviewers should insist on `< VERSION_X` comparisons for all protocol version dispatch, and should trace both branches to confirm each sends a verb the receiver actually recognises.

---

## Test

No test added.
