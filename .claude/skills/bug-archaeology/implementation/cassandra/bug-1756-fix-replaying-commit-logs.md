# Bug 1756: Fix Replaying Pre-2.0 Commit Logs

**Commit**: ad3a4f824e · **Ticket**: CASSANDRA-6714
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/CommitLogDescriptor.java, CommitLogReplayer.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`CommitLogReplayer` captured the messaging version into a local variable `version` but then used that variable only for the checksum-format branch; the deserialization call always used a hard-wired `version` variable that actually pointed at the local (same) variable — but the real problem was that `CommitLogDescriptor.getMessagingVersion()` contained an assertion `assert MessagingService.current_version == MessagingService.VERSION_20` that fired when replaying pre-2.0 commit log files during an upgrade. Additionally, the checksum branch checked `version < VERSION_20` using the local variable that was populated via `desc.getMessagingVersion()`, which would abort before reaching that code due to the assertion. The fix removes the assertion, exposes a `getVersion()` accessor on the descriptor, and rewrites the replayer to use `desc.getVersion()` for the checksum-format decision and `desc.getMessagingVersion()` for deserialization, properly handling all historical formats.

### Before / After

```java
// BEFORE (CommitLogDescriptor.getMessagingVersion)
public int getMessagingVersion() {
    assert MessagingService.current_version == MessagingService.VERSION_20; // fires pre-2.0 CL
    switch (version) { ... }
}

// BEFORE (CommitLogReplayer)
int version = desc.getMessagingVersion(); // assertion trips here for old CL files
...
if (version < CommitLogDescriptor.VERSION_20)
    checksum.update(serializedSize);
```

```java
// AFTER (CommitLogDescriptor)
public int getMessagingVersion() {
    switch (version) { ... } // assertion removed
}
public int getVersion() { return version; }

// AFTER (CommitLogReplayer)
if (desc.getVersion() < CommitLogDescriptor.VERSION_20)
    checksum.update(serializedSize);
...
rm = RowMutation.serializer.deserialize(..., desc.getMessagingVersion(), ...);
```

---

## How to Spot

Assertions that enforce "current version only" in format-parsing code will fire when the code is used to read historical data during upgrades. Deserialization helpers must be version-agnostic. Review any `assert currentVersion == X` inside a method that can be called with older persisted data.

---

## Test

No test added.
