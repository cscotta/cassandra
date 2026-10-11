**Commit**: 8533c43c9d · **Ticket**: KAFKA-18876
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
1. add prefix "config" to the commands' properties
2. add missed sections (6.11 and 6.12)
3. fix some incorrect commands

Reviewers: David Jacot <djacot@confluent.io>, Ken Huang <s7133700@gmail.com>, TengYao Chi <kitingiao@gmail.com>, Jun Rao <junrao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  <pre><code class="language-bash">$ bin/kafka-storage.sh format --cluster-id uuid --config server_properties</code></pre>
  <pre><code class="language-bash">$ bin/kafka-server-start.sh server_properties</code></pre>
  <pre><code class="language-bash">$ bin/kafka-storage.sh format --cluster-id &lt;cluster-id&gt; --standalone --config controller.properties</code></pre>
  <pre><code class="language-bash">cluster-id=$(bin/kafka-storage.sh random-uuid)
controller-0-uuid=$(bin/kafka-storage.sh random-uuid)
controller-1-uuid=$(bin/kafka-storage.sh random-uuid)
controller-2-uuid=$(bin/kafka-storage.sh random-uuid)
bin/kafka-storage.sh format --cluster-id ${cluster-id} \
                     --initial-controllers "0@controller-0:1234:${controller-0-uuid},1@controller-1:1234:${controller-1-uuid},2@controller-2:1234:${controller-2-uuid}" \
                     --config controller.properties</code></pre>
  <pre><code class="language-bash">$ bin/kafka-storage.sh format --cluster-id &lt;cluster-id&gt; --config server.properties --no-initial-controllers</code></pre>

// After
  <pre><code class="language-bash">$ bin/kafka-storage.sh format --cluster-id uuid --config config/server.properties</code></pre>
  <pre><code class="language-bash">$ bin/kafka-server-start.sh config/server.properties</code></pre>
  <pre><code class="language-bash">$ bin/kafka-storage.sh format --cluster-id &lt;CLUSTER_ID&gt; --standalone --config config/controller.properties</code></pre>
  <pre><code class="language-bash">CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_0_UUID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_1_UUID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_2_UUID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format --cluster-id ${CLUSTER_ID} \
                     --initial-controllers "0@controller-0:1234:${CONTROLLER_0_UUID},1@controller-1:1234:${CONTROLLER_1_UUID},2@controller-2:1234:${CONTROLLER_2_UUID}" \
                     --config config/controller.properties</code></pre>
  <pre><code class="language-bash">$ b
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18876 4.0 documentation improvement (#19065).

**Test**
No test added.
