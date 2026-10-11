**Commit**: ff58e90dfa · **Ticket**: KAFKA-19359
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Bump the commons-beanutils for CVE-2025-48734. Since `commons-validator`
hasn't had new release with newer `commons-beanutils` versions, we manually bump it in kafka. Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
- commons-beanutils-1.9.4
- commons-logging-1.3.2

// After
- commons-beanutils-1.11.0
- commons-logging-1.3.5
          // Workaround before `commons-validator` has new release. See KAFKA-19359.
          libs.commonsBeanutils,
  commonsBeanutils: "1.11.0",
  commonsBeanutils: "commons-beanutils:commons-beanutils:$versions.commonsBeanutils",
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19359: force bump commons-beanutils for cve-2025-48734 (#19939).

**Test**
No test added.
