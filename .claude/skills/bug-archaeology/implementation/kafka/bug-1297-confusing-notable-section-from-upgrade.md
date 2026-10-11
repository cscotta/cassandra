**Commit**: efa6f7ff47 · **Ticket**: KAFKA-18993
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Currently, the "Notable changes in 4.0.0" for the client is very confusing. We should remove it. Reviewers: mingdaoy <mingdaoy@gmail.com>, Luke Chen <showuon@gmail.com>, Ken Huang <s7133700@gmail.com>, David Jacot <djacot@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
<h6><a id="upgrade_clients_400_notable" href="#upgrade_clients_400_notable">Notable changes in 4.0.0</a></h6>

<ul>
    <li>Please see notable changes in the server section.</li>
</ul>


// After
// (no additions)
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18993 remove confusing notable change section from upgrade.html (#19212).

**Test**
No test added.
