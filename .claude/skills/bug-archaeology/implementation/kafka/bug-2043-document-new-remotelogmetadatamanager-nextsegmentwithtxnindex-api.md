**Commit**: 6dddabb359 · **Ticket**: KAFKA-16780
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
// (no removals)

// After
                <li>
                    For implementors of RemoteLogMetadataManager (RLMM), a new API `nextSegmentWithTxnIndex` is
                    introduced in RLMM to allow the implementation to return the next segment metadata with a transaction
                    index. This API is used when the consumers are enabled with isolation level as READ_COMMITTED.
                    See <a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-1058:+Txn+consumer+exerts+pressure+on+remote+storage+when+collecting+aborted+transactions">KIP-1058</a> for more details.
                </li>
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16780: document the new remotelogmetadatamanager#nextsegmentwithtxnindex api in upgrade notes. (#17735).

**Test**
No test added.
