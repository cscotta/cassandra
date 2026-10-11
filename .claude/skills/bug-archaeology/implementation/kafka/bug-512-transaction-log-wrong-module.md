## Bug 512 — Test infrastructure: TransactionLog moved to transaction-coordinator module

**Commit:** 4d81de035e
**Ticket:** KAFKA-18883
**Subsystem:** server / transaction-coordinator
**Category:** Logic error in condition

### Root cause
`TransactionLog` lived in the `core` module but logically belonged to the `transaction-coordinator` module. Tests and production code in `transaction-coordinator` had to reach back into `core` to access it, creating a circular dependency. Moving the class fixed the module boundary and allowed `transaction-coordinator` tests to access transaction log internals without violating the layering.

### Before
```
core module: TransactionLog.scala
transaction-coordinator module: imports from core — layering violation
```

### After
```
transaction-coordinator module: TransactionLog.java  (moved, rewritten in Java)
core module: no longer owns TransactionLog
```

### How to spot
Classes that are imported across module boundaries in the wrong direction (a lower-level module importing from a higher-level module) indicate misplaced ownership. Checkstyle import-control files are the authoritative source of truth.

### Test
Existing transaction log unit tests moved alongside the class.
