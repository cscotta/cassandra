**Commit**: e828767062 · **Ticket**: KAFKA-18790
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Frequently updating the trust store can cause unexpected termination of the AsyncConsumer background thread. 1. To resolve this issue, reuse the same AdminClient instead of recreating it. 2.

**Before/After**
```java
// Before
import scala.collection.mutable.ArrayBuffer
import scala.util.Using
  private val adminClients = new ArrayBuffer[Admin]()
    adminClients.foreach(_.close())
    Using.resource(createAdminClient()) { admin =>
      TestUtils.deleteTopicWithAdmin(
        admin = admin,
        topic = topic,
        brokers = aliveBrokers,
        controllers = controllerServers)
    }
    val adminClient = Admin.create(config)

// After
        } catch (final Throwable e) {
            log.error("Failed to initialize resources for consumer network thread", e);
  private var admin: Admin = _
    if (admin != null) admin.close()
    TestUtils.deleteTopicWithAdmin(
      admin = createAdminClient(),
      topic = topic,
      brokers = aliveBrokers,
      controllers = controllerServers)
    if (admin != null) return admin
    admin = Admin.create(config)
    admin
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18790 fix testcustomquotacallback (#18906).

**Test**
Test modified in CustomQuotaCallbackTest.scala covering the corrected behaviour.
