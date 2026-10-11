**Commit**: 2836f7aaae · **Ticket**: KAFKA-14705
**Subsystem**: core (GroupMetadataManager, TransactionLog, DumpLogSegments)
**Category**: State not cleaned up

**Root cause**
`OffsetsMessageFormatter`, `GroupMetadataMessageFormatter`, `TransactionLogMessageFormatter`, and the Scala `Decoder` trait were deprecated log-dump utilities that had been superseded by the protocol-based formatter framework. Their continued presence required maintaining a `newDecoder`/`convertDeprecatedDecoderClass` compatibility shim in `DumpLogSegments`. Removing the dead formatters eliminated the shim, two hundred lines of compatibility code, and a public API surface that users might mistakenly rely on.

**Before/After**
```scala
// Before – deprecated formatter kept with shim
class GroupMetadataMessageFormatter extends MessageFormatter { ... }
// DumpLogSegments:
val decoder = convertDeprecatedDecoderClass(className)

// After – formatter and shim removed
// DumpLogSegments: no decoder shim
```

**How to spot**
Search for `@deprecated` annotations in formatter/decoder classes and trace whether a compatibility shim in `DumpLogSegments` or similar entrypoints still instantiates them; if the only caller is the shim, both the deprecated class and the shim can be removed together.

**Test**
No test added.
