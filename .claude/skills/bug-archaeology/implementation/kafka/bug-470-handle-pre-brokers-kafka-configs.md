**Commit**: 5acd1ffe20 · **Ticket**: KAFKA-20111
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
KIP-1142 introduced `Admin.listConfigResources()` for listing resources
which have configurations, but which may not actually exist as run-time
entities such as consumer groups which have not yet had any members. Internally, this works by using v1 of the `LIST_CONFIG_RESOURCES` RPC
which is only supported in AK 4.1 and later. As a result, if you try to
describe the config for groups with older brokers, they do not support
v1 of the RPC and fail with `UnsupportedVersionException`. This PR
handles the exception in the config tool since the exception is harmless
and gives the same behaviour as we used to get with earlier versions.

**Before/After**
```java
// Before
import org.apache.kafka.common.errors.{InvalidConfigurationException, UnsupportedVersionException}
            if (adminClient.listGroups().all.get.stream.noneMatch(_.groupId() == name) &&
              adminClient.listConfigResources(java.util.Set.of(ConfigResource.Type.GROUP), new ListConfigResourcesOptions).all.get
                .stream.noneMatch(_.name == name)) {
          adminClient.listGroups().all.get.asScala.map(_.groupId).toSet ++
            adminClient.listConfigResources(java.util.Set.of(ConfigResource.Type.GROUP), new ListConfigResourcesOptions).all().get().asScala.map(_.name).toSet

// After
import org.apache.kafka.common.errors.{ClusterAuthorizationException, InvalidConfigurationException, UnsupportedVersionException}
            if (adminClient.listGroups().all.get.stream.noneMatch(_.groupId == name) && listGroupConfigResources(adminClient).exists(resources => resources.stream.noneMatch(_.name == name))) {
          adminClient.listGroups().all.get.asScala.map(_.groupId).toSet ++ listGroupConfigResources(adminClient).map(resources => resources.asScala.map(_.name).toSet).getOrElse(Set.empty)
  private def listGroupConfigResources(adminClient: Admin): Option[java.util.Collection[ConfigResource]] = {
    try {
      Some(adminClient.listConfigResources(java.util.Set.of(ConfigResource.Type.GROUP), new ListConfigResourcesOptions).all.get)
    } catch {
      // (KIP-1142) 4.1+ admin client vs older broker: treat UnsupportedVersionException and ClusterAuthorizationException as None
      case e: ExecutionException if e.getCause.isInstanceOf[UnsupportedVersionException] => None
      case e: ExecutionException if e.getCause.isInstanceOf[ClusterAuthorizationException] => None
      case e: ExecutionException => throw e.getCause
    }
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20111 handle pre-4.1 brokers in kafka-configs.sh for groups (#21385).

**Test**
Test modified in ConfigCommandTest.java covering the corrected behaviour.
