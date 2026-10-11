**Commit**: 9620866444 · **Ticket**: KAFKA-17152
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.zookeeper.server.ZooKeeperServer
import org.apache.zookeeper.server.NIOServerCnxnFactory

// After
import org.apache.zookeeper.server.{NIOServerCnxnFactory, SessionTrackerImpl, ZooKeeperServer}
    zookeeper.getSessionTracker match {
      case tracker: SessionTrackerImpl =>
        while (tracker.isAlive) {
          Thread.sleep(100)
        }
      case _ =>
    }

```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17152 make sure zk session tracker gets down when closing embedded zk (#16609).

**Test**
Test modified in EmbeddedZookeeper.scala covering the corrected behaviour.
