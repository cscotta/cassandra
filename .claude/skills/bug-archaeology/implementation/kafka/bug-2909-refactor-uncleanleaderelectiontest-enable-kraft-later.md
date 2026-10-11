**Commit**: 53b048bf0b · **Ticket**: KAFKA-15718
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Refactor UncleanLeaderElectionTest to allow to enable KRaft later

Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.config.{ConfigException, ConfigResource}
import org.junit.jupiter.api.{AfterEach, BeforeEach, Test, TestInfo}

import org.apache.log4j.{Level, Logger}
import java.util.Properties
import java.util.concurrent.ExecutionException

import kafka.server.{KafkaConfig, KafkaServer}
import kafka.server.QuorumTestHarness
import org.apache.kafka.common.errors.TimeoutException
import org.apache.kafka.clients.admin.{Admin, AdminClientConfig, AlterConfigsResult, Config, ConfigEntry}


// After
import java.util.Properties
import java.util.concurrent.ExecutionException
import kafka.server.{KafkaBroker, KafkaConfig, QuorumTestHarness}
import org.apache.kafka.common.config.{ConfigResource, TopicConfig}
import org.apache.kafka.common.errors.{InvalidConfigurationException, TimeoutException}
import org.apache.kafka.common.network.ListenerName
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.clients.admin.{Admin, AdminClientConfig, AlterConfigOp, AlterConfigsResult, ConfigEntry}
import org.apache.kafka.server.metrics.KafkaYammerMetrics
import org.apache.log4j.{Level, Logger}
import org.junit.jupiter.api.{AfterEach, BeforeEach, TestInfo}
import org.junit.jupiter.params.ParameterizedTest
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15718: refactor uncleanleaderelectiontest to enable kraft later (#16157).

**Test**
Test modified in UncleanLeaderElectionTest.scala covering the corrected behaviour.
