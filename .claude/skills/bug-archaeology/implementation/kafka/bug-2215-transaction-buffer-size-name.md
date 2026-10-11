**Commit**: d74f1f847a · **Ticket**: KAFKA-17199
**Subsystem**: server/transaction (TransactionLogConfig, TransactionCoordinator)
**Category**: Wrong constant / default value

**Root cause**
A configuration field was named `transactionsLoadBufferSize` (plural) while the corresponding config key and all documentation used the singular form `transactionLoadBufferSize`. The mismatch meant callers could easily reference the wrong accessor and retrieve a default value instead of the configured one, causing silently misconfigured transaction log loading.

**Before/After**
```java
// Before
public int transactionsLoadBufferSize() { ... }
// call site
int bufSize = config.transactionsLoadBufferSize();

// After
public int transactionLoadBufferSize() { ... }
// call site
int bufSize = config.transactionLoadBufferSize();
```

**How to spot**
When a config accessor name diverges from the config key string (e.g., singular vs. plural), the compiler cannot catch the mismatch; use grep to verify that every `config.get*` call matches the canonical key name in the config definition.

**Test**
New unit tests added in `TransactionCoordinatorTest` covering the corrected accessor.
