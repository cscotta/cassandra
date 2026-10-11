**Commit**: baa0fc9722 · **Ticket**: KAFKA-17042
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Luke Chen <showuon@gmail.com>, Chia-Ping Tsai <chia771@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    <li><a href="#brokerconfigs_broker.id">broker.id</a>: Ensure <code>broker.id</code> is set to a non-negative integer even if <code>broker.id.generation.enable</code> is enabled (default is enabled). Additionally, ensure <code>broker.id</code> does not exceed <code>reserved.broker.max.id</code> to avoid failure.</li>
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17042 the migration docs should remind users to set "broker.id.generation.enable" when adding broker.id (#16491).

**Test**
No test added.
