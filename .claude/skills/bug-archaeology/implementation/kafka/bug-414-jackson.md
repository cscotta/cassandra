**Commit**: 3a71651b0c · **Ticket**: KAFKA-20241
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
fix Jackson core vulnerability reported in

1. https://github.com/advisories/GHSA-72hv-8253-57qq
2. https://www.miggo.io/vulnerability-database/cve/GHSA-72hv-8253-57qq

Issue report is at https://issues.apache.org/jira/browse/KAFKA-20241

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
- jackson-annotations-2.20
- jackson-core-2.20.1
- jackson-databind-2.20.1
- jackson-dataformat-csv-2.20.1
- jackson-dataformat-yaml-2.20.1
- jackson-datatype-jdk8-2.20.1
- jackson-jakarta-rs-base-2.20.1
- jackson-jakarta-rs-json-provider-2.20.1
- jackson-module-blackbird-2.20.1
- jackson-module-jakarta-xmlbind-annotations-2.20.1
- snakeyaml-2.4
  jackson: "2.20.1",

// After
- jackson-annotations-2.21
- jackson-core-2.21.1
- jackson-databind-2.21.1
- jackson-dataformat-csv-2.21.1
- jackson-dataformat-yaml-2.21.1
- jackson-datatype-jdk8-2.21.1
- jackson-jakarta-rs-base-2.21.1
- jackson-jakarta-rs-json-provider-2.21.1
- jackson-module-blackbird-2.21.1
- jackson-module-jakarta-xmlbind-annotations-2.21.1
- snakeyaml-2.5
  jackson: "2.21.1",
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20241: bump jackson to 2.21.1 (#21621).

**Test**
No test added.
