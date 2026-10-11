**Commit**: 22d520adae · **Ticket**: KAFKA-19943
**Subsystem**: docs/streams
**Category**: State not cleaned up

**Root cause**
Implementation for KIP-1259

Adds a new property to set max directory age, and remove directories if
`current time - max age > last modify time`. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
`600000`
</td> (10 minutes)

// After
`600000` (10 minutes)
</td> </tr>
<tr>
<td>

state.cleanup.dir.max.age.ms
</td>
<td>

Low
</td>
<td>
```

**How to spot**
When reviewing docs/streams code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19943: implement state store clean up on start (#21566).

**Test**
Test modified in KafkaStreamsTest.java covering the corrected behaviour.
