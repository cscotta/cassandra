**Commit**: 66b1f00c0e · **Ticket**: KAFKA-18520
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.KafkaException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.Configuration;

    private static final Logger LOG = LoggerFactory.getLogger(JaasUtils.class);
    public static final String ZK_SASL_CLIENT = "zookeeper.sasl.client";
    public static final String ZK_LOGIN_CONTEXT_NAME_KEY = "zookeeper.sasl.clientconfig";

    private static final String DEFAULT_ZK_LOGIN_CONTEXT_NAME = "Client";

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18520: remove zookeeper logic from jaasutils (#18530).

**Test**
No test added.
