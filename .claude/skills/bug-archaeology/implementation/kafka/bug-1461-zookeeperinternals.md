**Commit**: a3d9d881e1 · **Ticket**: KAFKA-18530
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Since zk has been removed in 4.0, config handlers no longer need to handle the "<default>" value. This PR streamlines the config update process by eliminating the unnecessary string checks for "<default>"

Reviewers: Christo Lolov <lolovc@amazon.com>, Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
          if (!DynamicConfig.Ip.isValidIpEntity(ipEntity))
import org.apache.kafka.server.config.{ClientQuotaManagerConfig, ZooKeeperInternals}

    override def name: String = ZooKeeperInternals.DEFAULT_STRING
    override def name: String = ZooKeeperInternals.DEFAULT_STRING
      case DefaultUserEntity => ZooKeeperInternals.DEFAULT_STRING
    clientSensors.quotaSensor.record(value * (-1), timeMs, false)
   * @param sanitizedUser     user to override if quota applies to <user> or <user, client-id>
   * @param clientId          client to override if quota applies to <client-id> or <user, client-id>
   * @param sanitizedClientId sanitized client ID to override if quota applies to <client-id> or <user, client-id>
   * @param quota             custom quota to apply or None if quota override is being removed
  def updateQuota(sanitizedUser: Option[String], clientId: Option[String], sanitizedClientId:

// After
import java.net.{InetAddress, UnknownHostException}
          if (!isValidIpEntity(ipEntity))

  def isValidIpEntity(ip: String): Boolean = {
    try {
      InetAddress.getByName(ip)
    } catch {
      case _: UnknownHostException => return false
    }
    true
  }
import org.apache.kafka.server.config.ClientQuotaManagerConfig
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18530 remove zookeeperinternals (#18641).

**Test**
Test modified in DynamicBrokerReconfigurationTest.scala covering the corrected behaviour.
