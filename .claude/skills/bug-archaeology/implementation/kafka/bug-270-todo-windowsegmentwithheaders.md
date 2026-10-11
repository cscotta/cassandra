**Commit**: 82869a77bb · **Ticket**: KAFKA-20334
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Current implementation format for WindowSegmentWithHeaders and
RocksDBMigratingWindowStoreWithHeaders is
`[headersSize(varint)][headersBytes][timestamp(8)][value]`. Add todo to
mention the expected format will be achieved in KAFKA-20334. Reviewers: TengYao Chi <frankvicky@apache.org>

Signed-off-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
 * TODO: Current format is {@code [headersSize(varint)][headersBytes][timestamp(8)][value]}.
 *       The new format will be introduced in KAFKA-20334.
 * TODO: Current format is {@code [headersSize(varint)][headersBytes][timestamp(8)][value]}.
 *       The new format will be introduced in KAFKA-20334.
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: add todo to windowsegmentwithheaders (#21917).

**Test**
No test added.
