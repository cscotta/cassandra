**Commit**: 13d9a199f2 · **Ticket**: KAFKA-18011
**Subsystem**: docs/configuration.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      <li><code>broker.id</code>
      <li><code>zookeeper.connect</code>
  <li>Dynamic per-broker config stored in ZooKeeper</li>
  <li>Dynamic cluster-wide default config stored in ZooKeeper</li>
  <h5>Updating Password Configs Dynamically</h5>
  <p>Password config values that are dynamically updated are encrypted before storing in ZooKeeper. The broker config
  <code>password.encoder.secret</code> must be configured in <code>server.properties</code> to enable dynamic update
  of password configs. The secret may be different on different brokers.</p>
  <p>The secret used for password encoding may be rotated with a rolling restart of brokers. The old secret used for encoding
  passwords currently in ZooKeeper must be provided in the static broker config <code>password.encoder.old.secret</code> and
  the new secret must be provided in <code>password.encoder.secret</code>. All dynamic password configs stored in ZooKeeper
  will be re-encoded with the new secret when the broker starts up.</p>

// After
      <li><code>node.id</code>
      <li><code>process.roles</code>
      <li><code>controller.quorum.bootstrap.servers</code>
  <li>Dynamic per-broker config stored in the metadata log</li>
  <li>Dynamic cluster-wide default config stored in the metadata log</li>
```

**How to spot**
When reviewing docs/configuration.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18011 remove zookeeper sections from the docs (#17813).

**Test**
No test added.
