**Commit**: 7b23976e78 · **Ticket**: KAFKA-17000
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Viktor Somogyi-Vass <viktorsomogyi@gmail.com>, Dániel Urbán <urb.daniel7@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.metadata.authorizer.StandardAuthorizerTest.AuthorizerTestServerInfo
import org.apache.kafka.server.config.ZkConfigs
import org.apache.kafka.server.config.ReplicationConfigs
    authorizer1 = createAuthorizer(config.originals)
    authorizer2 = createAuthorizer(config.originals)
    var testAuthorizer: Authorizer = null
      testAuthorizer = createAuthorizer(cfg.originals)
    var testAuthorizer: Authorizer = null
      testAuthorizer = createAuthorizer(cfg.originals)
    var authorizer: Authorizer = null
      authorizer = createAclAuthorizer(config.originals)
    var authorizer: Authorizer = null

// After
import org.apache.kafka.metadata.authorizer.StandardAuthorizerTest.AuthorizerTestServerInfo
import org.apache.kafka.server.config.{ReplicationConfigs, ZkConfigs}
    authorizer1 = createAuthorizer()
    configureAuthorizer(authorizer1, config.originals)
    authorizer2 = createAuthorizer()
    configureAuthorizer(authorizer2, config.originals)
    val testAuthorizer = createAuthorizer()
      configureAuthorizer(testAuthorizer, cfg.originals)
    val testAuthorizer = createAuthorizer()
      configureAuthorizer(testAuthorizer, cfg.originals)
    val authorizer = new AclAuthorizer
      configureAclAuthorizer(authorizer, config.originals)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17000 separating creation and configuration of authorizer in authorizertest (#16401).

**Test**
Test modified in AuthorizerTest.scala covering the corrected behaviour.
