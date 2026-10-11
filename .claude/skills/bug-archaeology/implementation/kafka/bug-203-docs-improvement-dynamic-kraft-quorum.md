**Commit**: 63cc2f327f · **Ticket**: KAFKA-18149
**Subsystem**: docs/operations
**Category**: Logic error in condition

**Root cause**
* be specific that --command-config for add-controller needs to point to
the to-be-added controller's config
* be specific that `controller.quorum.bootstrap.servers` in the
to-be-added controllers config needs to contain a controller in the
quorum

Reviewers: Mickael Maison <mimaison@apache.org>, Alyssa Huang
<ahuang@confluent.io>

---------

Signed-off-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
The `--command-config` needs to point to the to-be-added controller's configuration file.

Be sure that the `controller.quorum.bootstrap.servers` property in the specified `--command-config` file contains at least one controller currently in the quorum.

```

**How to spot**
When reviewing docs/operations code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18149: docs improvement for dynamic kraft quorum (#18350).

**Test**
No test added.
