**Commit**: 400ecab518 · **Ticket**: KAFKA-13810
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Luke Chen <showuon@gmail.com>, Andrew Eugene Choi <andrew.choi@uwaterloo.ca>

**Before/After**
```java
// Before
     * of <code>flush()</code> is that any previously sent record will have completed (e.g. <code>Future.isDone() == true</code>).

// After
     * of <code>flush()</code> is that any previously sent record will have completed (e.g. <code>Future.isDone() == true</code>
     * and callbacks passed to {@link #send(ProducerRecord,Callback)} have been called).
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13810: document behavior of kafkaproducer.flush() w.r.t callbacks (#12042).

**Test**
No test added.
