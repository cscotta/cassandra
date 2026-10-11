## Commit

- Hash: fcd98da9ae
- Tickets: KAFKA-18445
- Subject: KAFKA-18445: Remove LazyDownConversionRecords and LazyDownConversionRecordsSend dead code

## Subsystem

core / network / down-conversion

## Category

Resource leak

## Root cause

`LazyDownConversionRecords` and `LazyDownConversionRecordsSend` were created to lazily down-convert message formats for older clients. After down-conversion was removed from the data path (clients must now handle their own format negotiation), these classes became unreachable dead code. `MultiRecordsSend` still carried a `recordConversionStats()` method and the broker still called `updateRecordConversionStats()`, maintaining an unused stats pipeline that wasted memory and CPU cycles.

## Before

`LazyDownConversionRecords.java`, `LazyDownConversionRecordsSend.java` present; `MultiRecordsSend.recordConversionStats()` and `updateRecordConversionStats()` wiring active.

## After

Both files deleted; `recordConversionStats()` and `updateRecordConversionStats()` removed from `MultiRecordsSend`; callers updated.

## How to spot

`LazyXxx` wrapper classes that exist to defer a transformation that is no longer applied in the current code path — the lazy wrapper itself becomes dead code when the transformation it wraps is removed.

## Test

No functional change; existing fetch and replication tests pass without the deleted code.
