**Commit**: e4795bfc37 · **Ticket**: KAFKA-19900

**Subsystem**: docs/ops.html

**Category**: Wrong constant/default value

**Root cause**
The operations documentation for removing a KRaft controller node still referenced KIP-996 as a future capability with a conditional note about when it would be available. KIP-996 shipped in Apache Kafka 4.0.0, so the conditional language was incorrect: operators following the docs would see a caveat implying the feature might not be present and might attempt an incorrect removal sequence. The fix removes the KIP-996 reference and presents the "remove controller first, then shut down" sequence as the authoritative procedure for all supported releases.

**Before**
```html
<!-- Before: conditional note implying KIP-996 may not be present -->
<p>If KIP-996 is available, remove the controller first before shutting down...</p>
```

**After**
```html
<!-- After: unconditional procedure, KIP-996 mention removed -->
<p>Remove the controller first, then shut it down.</p>
```

**How to spot**
Search `docs/ops.html` for mentions of KIP numbers alongside conditional language ("if available", "once implemented"); any KIP that shipped in a release older than the minimum supported version is a stale caveat that should be unconditionalised.

**Test**
Review the rendered `ops.html` and confirm the controller-removal section gives clear, unconditional instructions without referencing KIP-996 as a future or conditional capability.
