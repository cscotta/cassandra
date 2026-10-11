**Commit**: c40cc5740f · **Ticket**: KAFKA-18408
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    { "name": "OfflineLogDirs", "type":  "[]uuid", "versions": "1+", "taggedVersions": "1+", "tag": "0",
   { "name": "LogDirs", "type":  "[]uuid", "versions":  "2+", "taggedVersions": "2+", "tag": "2",
    { "name": "LogDirs", "type":  "[]uuid", "versions":  "3+", "taggedVersions": "3+", "tag": "0",

// After
    { "name": "OfflineLogDirs", "type":  "[]uuid", "versions": "1+", "taggedVersions": "1+", "tag": 0,
   { "name": "LogDirs", "type":  "[]uuid", "versions":  "2+", "taggedVersions": "2+", "tag": 2,
    { "name": "LogDirs", "type":  "[]uuid", "versions":  "3+", "taggedVersions": "3+", "tag": 0,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18408 tweak the 'tag' field for brokerheartbeatrequest.json, brokerregistrationchangerecord.json and registerbroke.

**Test**
No test added.
