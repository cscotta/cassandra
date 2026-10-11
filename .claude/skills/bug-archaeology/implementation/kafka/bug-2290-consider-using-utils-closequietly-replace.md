**Commit**: 8be808ea4a · **Ticket**: KAFKA-17285
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      finally CoreUtils.swallow(authZ.close(), this)
    CoreUtils.swallow(channel.socket().close(), logging, Level.ERROR)
    CoreUtils.swallow(channel.close(), logging, Level.ERROR)
    if (serverChannel != null) {
      CoreUtils.swallow(serverChannel.close(), this, Level.ERROR)
    }
    CoreUtils.swallow(nioSelector.close(), this, Level.ERROR)
import kafka.utils.{CoreUtils, Logging}
import org.apache.kafka.common.utils.Time
        CoreUtils.swallow(reader.close(), logging)
    CoreUtils.swallow(expirationTimer.close(), this)
    CoreUtils.swallow(netChannel.close(), this)

// After
      finally Utils.closeQuietly(authZ, "authorizer")
    Utils.closeQuietly(channel.socket, "channel socket")
    Utils.closeQuietly(channel, "channel")
    Utils.closeQuietly(serverChannel, "Acceptor serverChannel")
    Utils.closeQuietly(nioSelector, "Acceptor nioSelector")
import kafka.utils.Logging
import org.apache.kafka.common.utils.{Time, Utils}
        Utils.closeQuietly(reader, "reader")
    Utils.closeQuietly(expirationTimer, "expiration timer")
    Utils.closeQuietly(netChannel, "net channel")
    Utils.closeQuietly(replicatedLog, "replicated log")
import org.apache.kafka.common.utils.{LogContext, Time, Utils}
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17285 consider using `utils.closequietly` to replace `coreutils.swallow` when handling closeable objects (#16843).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
