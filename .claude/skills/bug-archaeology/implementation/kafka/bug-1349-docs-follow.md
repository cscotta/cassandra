**Commit**: 0b6a42d437 · **Ticket**: KAFKA-18876
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
https://github.com/apache/kafka/pull/19065#issuecomment-2698614575

Fixed:

![image](https://github.com/user-attachments/assets/2995f962-298f-4a4c-8f52-0b0459fd3b00)

Reviewers: Ken Huang <s7133700@gmail.com>, TengYao Chi <kitingiao@gmail.com>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
  <pre><code class="language-bash">$ bin/kafka-metadata-shell.sh --snapshot metadata_log_dir/__cluster_metadata-0/00000000000000000000.log

// After
  <pre><code class="language-bash">$ bin/kafka-metadata-shell.sh --snapshot metadata_log_dir/__cluster_metadata-0/00000000000000000000.checkpoint
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18876: 4.0 docs follow-up (#19103).

**Test**
No test added.
