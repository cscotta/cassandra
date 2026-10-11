**Commit**: 5d81fe20c8 · **Ticket**: KAFKA-18590
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      tokenManager.startup()
      if (tokenManager != null)
        CoreUtils.swallow(tokenManager.shutdown(), this)

import java.nio.ByteBuffer

import org.apache.kafka.common.protocol.Errors
  val CurrentVersion = 3
  /**
   *
   * @param tokenId
   * @param secretKey

// After
import java.util
import java.util.stream.Collectors
    val hmacString = token.hmacAsBase64String
    val scramCredentialMap = prepareScramCredentials(hmacString)
    tokenCache.updateCache(token, scramCredentialMap.asJava)
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18590 cleanup delegationtokenmanager (#18618).

**Test**
No test added.
