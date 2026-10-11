**Commit**: 8b49130b92 · **Ticket**: KAFKA-19355
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Following the removal of the ZK-to-KRaft migration code in commit
85bfdf4, controller-to-broker communication is now handled by the
control-plane listener (`controller.listener.names`). The
`interBrokerListenerName` parameter in `ClusterControlManager` is no
longer referenced on the controller side and can be safely removed as
dead code. Reviewers: Lan Ding <isDing_L@163.com>, Ken Huang <s7133700@gmail.com>,
Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
          setInterBrokerListenerName(config.interBrokerListenerName.value()).
        private String interBrokerListenerName = "PLAINTEXT";
        Builder setInterBrokerListenerName(String interBrokerListenerName) {
            this.interBrokerListenerName = interBrokerListenerName;
            return this;
        }
                interBrokerListenerName,
    /**
     * The statically configured inter-broker listener name.
     */
    private final String interBrokerListenerName;


// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19355 remove interbrokerlistenername from clustercontrolmanager (#19866).

**Test**
No test added.
