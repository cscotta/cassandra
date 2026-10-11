**Commit**: b4258a0cb9 · **Ticket**: KAFKA-18070
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Ziming Deng <dengziming1993@gmail.com>.

**Before/After**
```java
// Before
NodeId  LogEndOffset    Lag     LastFetchTimestamp      LastCaughtUpTimestamp   Status
1       25806           0       1662500992757           1662500992757           Leader

// After
NodeId	DirectoryId           	LogEndOffset	Lag	LastFetchTimestamp	LastCaughtUpTimestamp	Status
1     	dDo1k_pRSD-VmReEpu383g	966         	0  	1732367153528     	1732367153528        	Leader
2     	wQWaQMJYpcifUPMBGeRHqg	966         	0  	1732367153304     	1732367153304        	Observer
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18070: update kafka-metadata-quorum.sh output in docs to match kip-853 (#17921).

**Test**
No test added.
