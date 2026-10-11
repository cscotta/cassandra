**Commit**: 963bc6a4b8 · **Ticket**: KAFKA-20373
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Bump maven-artifact to resolve plexus-utils CVE:
[CVE-2025-67030](https://www.cve.org/CVERecord?id=CVE-2025-67030). Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Mickael Maison
<mickael.maison@gmail.com>

**Before/After**
```java
// Before
- maven-artifact-3.9.11
- plexus-utils-3.6.0
  mavenArtifact: "3.9.11",

// After
- maven-artifact-3.9.15
- plexus-utils-3.6.1
  mavenArtifact: "3.9.15",
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20373: bump maven artifact to 3.9.15 (#21911).

**Test**
No test added.
