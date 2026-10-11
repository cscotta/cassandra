**Commit**: d0bca67b27 · **Ticket**: KAFKA-18363
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, TengYao Chi <kitingiao@gmail.com>

**Before/After**
```java
// Before
            "which is used to determine quota limits applied to client requests. By default, the &lt;user&gt; and &lt;client-id&gt; " +
            "quotas that are stored in ZooKeeper are applied. For any given request, the most specific quota that matches the user principal " +
            "of the session and the client-id of the request is applied.";
                    " use to connect, such as in cloud environments. In environments using ZooKeeper, these addresses are published to ZooKeeper." +
                    " In Kraft mode, the address would be published to and managed by kraft controller, the brokers would pull these data from controller as needed." +
    public static final String PROCESS_ROLES_DOC = "The roles that this process plays: 'broker', 'controller', or 'broker,controller' if it is both. " +
            "This configuration is only applicable for clusters in KRaft (Kafka Raft) mode (instead of ZooKeeper). Leave this config undefined or empty for ZooKeeper clusters.";

    public static final String BROKER_HEARTBEAT_INTERVAL_MS_DOC = "The length of time in milliseconds between broker heartbeats. Used when running in KRaft mode.";
    public static final String BROKER_SESSION_TIMEOUT_MS_DOC = "The length of time in milliseconds that a broker lease lasts if no heartbeats are made. Used when running in KRaft mode.";

// After
            "which is used to determine quota limits applied to client requests. " +
            "For any given request, the most specific quota that matches the user principal of the session and the client-id of the request is applied.";
                    " use to connect, such as in cloud environments." +
                    " The addresses are published to and managed by the controller, the brokers pull these data from the controller as needed." +
    public static final String PROCESS_ROLES_DOC = "The roles that this process plays: 'broker', 'controller', or 'broker,controller' if it is both. ";

    public static final String BROKER_HEARTBEAT_INTERVAL_MS_DOC = "The length of time in milliseconds between broker heartbeats.";
    public static final String BROKER_SESSION_TIMEOUT_MS_DOC = "The length of time in milliseconds that a broker lease lasts if no heartbeats are m
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18363: remove zookeeper mentiosn in broker configs (#18346).

**Test**
No test added.
