**Commit**: 3710add2a7 · **Ticket**: KAFKA-18012
**Subsystem**: docs/security.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
                stores SCRAM credentials in Zookeeper and is suitable for use in Kafka installations where Zookeeper
                is on a private network. Refer to <a href="#security_sasl_scram_security">Security Considerations</a>
                    <p>The SCRAM implementation in Kafka uses Zookeeper as credential store. Credentials can be created in
                        Zookeeper using <code>kafka-configs.sh</code>. For each SCRAM mechanism enabled, credentials must be created
                        before Kafka brokers are started. Client credentials may be created and updated dynamically and updated
                        credentials will be used to authenticate new connections.</p>
                    <p>Create SCRAM credentials for user <i>alice</i> with password <i>alice-secret</i>:
                    <pre><code class="language-bash">$ bin/kafka-configs.sh --zookeeper localhost:2182 --zk-tls-config-file zk_tls_config.properties --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=alice-secret],SCRAM-SHA-512=[password=alice-secret]' --entity-type users --entity-name alice</code></pre>
                    <p>The default iteration count of 4096 is used if iterations are not specified. A random salt is created
                        and the SCRAM identity consisting of salt, iterations, StoredKey and ServerKey are stored in Zookeeper.
                    <p>The following examples also require a user <i>admin</i> for inter-broker communication which can be created using:
                    <pre><code class="language-bash">$ bin/kafka-configs.sh --zookeeper localhost:2182 --zk-tls-config-file zk_tls_config.properties --alter --add-config 'SCRAM-SHA-256=[password=admin-secret],SCRAM-SHA-512=[password=admin-secret]' --entity-type users --entity-name admin</code></pre>

// After
                stores SCRAM credentials in the metadata log. Refer to <a href="#security_sasl_scram_security">Security Considerations</a>
                    <p>The SCRAM implementation in Kafka uses the metadata log as credential store. Credentials can be created in
                        the metadata log using <code>kafka-storage.sh</code> or <code>kafka-configs.sh</code>. For each SCRAM mechanism enabled, credentials must be created
                        before Kafka brokers are started. <code>kafka-storage.sh</code> can format storage with initial credentials.
                        Client credentials may be created and updated dynamically and updated credentials will be used to authenticate new connections.
                        <code>kafka-configs.sh</code> can be used to create and update credentials after Kafka brokers are started.</p>
                    <p>Create initial SCRAM credentials for user <i>admin</i> with password <i>admin-secret</i>:
                    <pre><code class="language-bash">$ bin/kafka-storage.sh format -t $(bin/kafka-storage.sh random-uuid) -c config/kraft/server.properties --add-scram 'SCRAM-SHA-256=[name="admin",password="admin-secret"]'</code></pre>
                    <p>Create SCRAM credentials for user <i>alice</i> with password <i>alice-secret</i> (refer to <a href="#security_sasl_scram_clientconfig">Configuring Kafka Clients</a> for client configuration):
                    <pre><code class="language-bash">$ bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=alice-secret]' --entity-type users --entity-name alice --command-config client.properties</code></pre>
                    <p>The default iteration count of 4096 is used if iterations are not specified. A random salt is created if it's not specified.
                        The SCRAM identity consisting of salt, iterations, StoredKey and ServerKey are stored in the metadata log.
```

**How to spot**
When reviewing docs/security.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18012: update the scram configuration section for kraft (#17844).

**Test**
No test added.
