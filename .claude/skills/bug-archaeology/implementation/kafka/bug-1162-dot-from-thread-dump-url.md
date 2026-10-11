**Commit**: 4410d35cdc · **Ticket**: KAFKA-19179
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
As the title. Ticket: https://issues.apache.org/jira/browse/KAFKA-19179

Reviewers: PoAn Yang <payang@apache.org>, Jhen-Yung Hsu
 <jhenyunghsu@gmail.com>, TengYao Chi <frankvicky@apache.org>, Nick Guo
 <lansg0504@gmail.com>, Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
        failure_messages.append(f"Thread dump available at {thread_dump_url}. Failing this script.")

// After
        failure_messages.append(f"Thread dump available at {thread_dump_url} and the script will now fail.")
```

**How to spot**
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19179: remove the dot from thread_dump_url (#19525).

**Test**
No test added.
