**Commit**: 9d6faf0283 · **Ticket**: KAFKA-18674
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
                        <li>The valid format for <code>--bootstrap-server</code> only supports comma-separated value, such as <code>host1:port1,host2:port2,...</code>.
                            Providing other formats, like space-separated bootstrap servers (e.g., <code>host1:port1 host2:port2 host3:port3</code>), will result in an exception, even though this was allowed in Apache Kafka versions prior to 4.0.
                        </li>
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18674 document the incompatible changes in parsing --bootstrap-server (#18751).

**Test**
No test added.
