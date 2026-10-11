**Commit**: a5e3bfa232 · **Ticket**: KAFKA-19527
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Clarifies that the fields `LogDirDescription#totalBytes`,
`LogDirDescription#usableBytes`, and `ReplicaInfo#size` do not include
the size of remote storage by updating their corresponding docs. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        "about": "The total size in bytes of the volume the log directory is in."
        "about": "The usable size in bytes of the volume the log directory is in."
}

// After
     * This value does not include the size of data stored in remote storage.
     * This value does not include the size of data stored in remote storage.
     * This value does not include the size of data stored in remote storage.
        "about": "The total size in bytes of the volume the log directory is in. This value does not include the size of data stored in remote storage."
        "about": "The usable size in bytes of the volume the log directory is in. This value does not include the size of data stored in remote storage."
}
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19527 improve the docs of logdirdescription for remote storage (#20211).

**Test**
No test added.
