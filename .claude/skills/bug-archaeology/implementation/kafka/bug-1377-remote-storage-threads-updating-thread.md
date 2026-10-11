**Commit**: 8bbca913ef · **Ticket**: KAFKA-18869
**Subsystem**: docs/configuration.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
    <li><code>remote.log.reader.threads</code></li>
    <li><code>remote.log.manager.copier.thread.pool.size</code></li>
    <li><code>remote.log.manager.expiration.thread.pool.size</code></li>
```

**How to spot**
When reviewing docs/configuration.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18869 add remote storage threads to "updating thread configs" section (#19037).

**Test**
No test added.
