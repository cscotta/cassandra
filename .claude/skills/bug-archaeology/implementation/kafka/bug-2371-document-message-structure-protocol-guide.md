**Commit**: ab1eb8d561 · **Ticket**: KAFKA-17494
**Subsystem**: docs/protocol.html
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Josep Prat <josep.prat@aiven.io>

**Before/After**
```java
// Before
// (no removals)

// After

<p>The message consists of the header and body:</p>
<pre>
Message => RequestOrResponseHeader Body
</pre>

<p><code>RequestOrResponseHeader</code> is the versioned request or response header. <code>Body</code> is the message-specific body.</p>

```

**How to spot**
When reviewing docs/protocol.html code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17494: document message structure in protocol guide (#17122).

**Test**
No test added.
