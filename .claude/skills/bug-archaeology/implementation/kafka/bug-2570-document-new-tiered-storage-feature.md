**Commit**: 03d1a736cb · **Ticket**: KAFKA-17300
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
- Added document for disabling tiered storage at topic level
- Added notable changes items in v3.9.0 for tiered storage quota

Reviewers: Satish Duggana <satishd@apache.org>, Kamal Chandraprakash<kamal.chandraprakash@gmail.com>, Abhijeet Kumar<abhijeet.cse.kgp@gmail.com>

**Before/After**
```java
// Before
<p>Please note, if you want to disable tiered storage at the cluster level, you should delete the tiered storage enabled topics explicitly.
  <li>No support for clusters with multiple log directories (i.e. JBOD feature)</li>
  <li>Cannot disable tiered storage at the topic level</li>


// After
<p>In KRaft mode, you can disable tiered storage at the topic level, to make the remote logs as read-only logs, or completely delete all remote logs.</p>

<p>If you want to let the remote logs become read-only and no more local logs copied to the remote storage,
  you can set <code>remote.storage.enable=true,remote.log.copy.disable=true</code> to the topic.</p>

<p>Note: You also need to set <code>local.retention.ms</code> and <code>local.retention.bytes</code> to the same value as
  <code>retention.ms</code> and <code>retention.bytes</code>, or set to "-2". This is because after disabling remote log copy,
  the local retention policies will not be applied anymore, and that might confuse users and cause unexpected disk full.
</p>

<pre><code class="language-bash">$ bin/kafka-configs.sh --bootstrap-server localhost:9092 \
   --alter --entity-type topics --entity-name tieredTopic \
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17300: add document for new tiered storage feature in v3.9.0. (#16836).

**Test**
No test added.
