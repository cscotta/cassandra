**Commit**: 33c5e84c9f · **Ticket**: KAFKA-7699
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Update `Notable changes in 4.2.0` in `docs/upgrade.html` with
information about
[KIP-1146](https://cwiki.apache.org/confluence/x/9QqWF). Reviewers: Bill Bejeck<bbejeck@apache.org>

---------

Co-authored-by: Bill Bejeck <bbejeck@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    <li>
        It is now possible to specify the start time for a Kafka Streams punctuation, instead of relying on the non-deterministic time when you register it.
        For further details, please refer to <a href="https://cwiki.apache.org/confluence/x/9QqWF">KIP-1146</a>.
    </li>
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-7699: kip-1146 notable change docs for 4.2.0 (#20903).

**Test**
No test added.
