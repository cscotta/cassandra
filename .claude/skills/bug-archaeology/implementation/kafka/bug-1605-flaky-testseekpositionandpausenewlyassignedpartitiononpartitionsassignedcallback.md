**Commit**: 4f0a91393c · **Ticket**: KAFKA-17623
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
    triggerOnPartitionsAssigned { (consumer, _) =>
    }
    triggerOnPartitionsAssigned { (consumer, _) =>
    }
    triggerOnPartitionsAssigned { (consumer, _) =>
    }
    triggerOnPartitionsRevoked { (consumer, _) =>
    }
    triggerOnPartitionsRevoked { (consumer, _) =>
    }
    triggerOnPartitionsRevoked { (consumer, _) =>
    }

// After
    triggerOnPartitionsAssigned(tp, { (consumer, _) =>
    })
    triggerOnPartitionsAssigned(tp, { (consumer, _) =>
    })
    triggerOnPartitionsAssigned(tp, { (consumer, _) =>
    })
    triggerOnPartitionsRevoked(tp, { (consumer, _) =>
    })
    triggerOnPartitionsRevoked(tp, { (consumer, _) =>
    })
    triggerOnPartitionsRevoked(tp, { (consumer, _) =>
    })
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17623: flaky testseekpositionandpausenewlyassignedpartitiononpartitionsassignedcallback (#18515).

**Test**
Test modified in PlaintextConsumerCallbackTest.scala covering the corrected behaviour.
