**Commit**: 375ed19fba · **Ticket**: KAFKA-19100
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Use the ProcessRole enum instead of hardcoding the role

Reviewers: Mickael Maison <mickael.maison@gmail.com>, PoAn Yang
<poan.yang@suse.com>, Jhen-Yung Hsu <jhenyunghsu@gmail.com>, Ken Huang
<s7133700@gmail.com>

**Before/After**
```java
// Before
              name: String,
  this.logIdent = "[AclApis-%s-%s] ".format(name, config.nodeId)
  private val aclApis = new AclApis(authHelper, authorizer, requestHelper, "controller", config)
import org.apache.kafka.server.ClientMetricsManager
  val aclApis = new AclApis(authHelper, authorizer, requestHelper, "broker", config)

// After
import org.apache.kafka.server.ProcessRole
              role: ProcessRole,
  this.logIdent = "[AclApis-%s-%s] ".format(role, config.nodeId)
import org.apache.kafka.server.ProcessRole
  private val aclApis = new AclApis(authHelper, authorizer, requestHelper, ProcessRole.ControllerRole, config)
import org.apache.kafka.server.{ClientMetricsManager, ProcessRole}
  val aclApis = new AclApis(authHelper, authorizer, requestHelper, ProcessRole.BrokerRole, config)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19100: use processrole instead of string in aclapis (#19406).

**Test**
No test added.
