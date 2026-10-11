**Commit**: 98d7618b94 · **Ticket**: KAFKA-17319
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Luke Chen <showuon@gmail.com>, PoAn Yang <payang@apache.org>, TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  "latestVersionUnstable": true,

// After
  "latestVersionUnstable": false,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17319 change listoffsetsrequest latestversionunstable to false (#16865).

**Test**
No test added.
