**Commit**: e42e01eec3 · **Ticket**: KAFKA-19184

**Subsystem**: Documentation / KRaft operations

**Category**: Misleading/wrong documentation or log message

**Root cause**
Apache Kafka 4.1 added support for upgrading a KRaft cluster from a static controller configuration (version 0) to a dynamic controller configuration (version 1), enabling controllers to be added and removed at runtime. However, the operations documentation (`docs/ops.html`) contained no instructions for how to perform this upgrade, leaving operators without guidance on the required steps or the CLI commands to use. The fix adds a new "Upgrade" section with subsections for describing the current KRaft version and executing the version upgrade.

**Before/After**
```html
<!-- Before: no KRaft upgrade section in ops.html -->

<!-- After: new section added -->
<h4 id="kraft_upgrade">Upgrade</h4>
<p>Apache Kafka 4.1 added support for upgrading a cluster from a
static controller configuration to a dynamic controller
configuration...</p>
<h5 id="kraft_upgrade_describe">Describe KRaft Version</h5>
<pre><code>$ bin/kafka-features.sh --bootstrap-controller localhost:9093 describe
</code></pre>
```

**How to spot**
After every KIP that introduces a new operational procedure (upgrade, migration, reconfiguration), verify that `docs/ops.html` contains a corresponding section with step-by-step instructions; absence means operators must reverse-engineer the procedure from source code.

**Test**
Documentation review confirms that `docs/ops.html` now includes the KRaft version upgrade procedure with CLI examples and a reference to the controller membership changes section.
