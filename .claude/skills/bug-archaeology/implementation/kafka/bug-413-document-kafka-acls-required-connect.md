**Commit**: 188a910d15 · **Ticket**: KAFKA-20239
**Subsystem**: docs/kafka-connect
**Category**: Logic error in condition

**Root cause**
Adding a list of permissions needed by Connect

I've tried to copy the style and wording in the ACL requirements table
already there for Exactly-once support. https: //issues.apache.org/jira/browse/KAFKA-20239

Reviewers: Andrew Schofield <aschofield@confluent.io>

---------

Signed-off-by: Dale Lane <dale.lane@uk.ibm.com>

**Before/After**
```java
// Before
If a sink connector supports exactly-once semantics, to enable exactly-once at the Connect worker level, you must ensure its consumer group is configured to ignore records in aborted transactions. You can do this by setting the worker property `consumer.isolation.level` to `read_committed` or, if running a version of Kafka Connect that supports it, using a connector client config override policy that allows the `consumer.override.isolation.level` property to be set to `read_committed` in individual connector configs. There are no additional ACL requirements.
#### ACL requirements
With exactly-once source support enabled, or with `exactly.once.source.support` set to `preparing`, the principal for each Connect worker will require the following ACLs:

<table>
<tr>
<th>
Operation
</th>
<th>
Resource Type
</th>

// After
If a sink connector supports exactly-once semantics, to enable exactly-once at the Connect worker level, you must ensure its consumer group is configured to ignore records in aborted transactions. You can do this by setting the worker property `consumer.isolation.level` to `read_committed` or, if running a version of Kafka Connect that supports it, using a connector client config override policy that allows the `consumer.override.isolation.level` property to be set to `read_committed` in individual connector configs.
## Plugin Discovery
Plugin discovery is the name for the strategy which the Connect worker uses to find plugin classes and make them accessible to configure and run in connectors. This is controlled by the plugin.discovery worker configuration, and has a significant impact on worker startup time. `service_load` is the fastest strategy, but care should be taken to verify that plugins are compatible before setting this configuration to `service_load`.
Prior to version 3.6, this strategy was not configurable, and behaved like the `only_scan` mode which is compatible with all plugins. For version 3.6 and later, this mode defaults to `hybrid_warn` which is also compatible with all plugins, but logs a warning for plugins which are incompatible with `service_load`. The `hybrid_fail` strategy stops the worker with an error if a plugin incompatible with `service_load` is detected, asserting that all plugins are compatible. Finally, the `service_load` strategy disables the slow legacy scanning mechanism used in all other modes, and instead uses the faster `ServiceLoader` mechanism. Plugins which are incompatible with that mechanism may be unusable.
### Verifying Plugin Compatibility
To verify if all of your plugins are compatible with `service_load`, first ensure that you are using version 3.6 or later of Kafka Connect. You can then perform one of the following checks:
  * Start your worker with the default `hybrid_warn`strategy, and WARN logs enabled for the `org.apache.kafka.connect` package. At least one WARN log message mentioning the `plugin.discovery` configuration should be printed. This log message will explicitly say that all plugins are compatible, or list the incompatible plugins.
  * Start your worker in a test environment with `hybrid_fail`. If all plugins are compatible, startup will succeed. If at least one plugin is not compatible the worker will fail to start up, and all incompatible plugins will be listed in the exception.
If the verification step succeeds, then your current set of installed plugins is compatible, and it should be safe to change the `plugin.discovery` configuration to `service_load`. If the verification fails, you cannot use `service_load` strategy and should take note of the list of incompatible plugins. All plugins must be addressed before using the `service_load` strategy. It is recommended to perform this verification after installing or changing plugin versions, and the verification can be done automatically in a Continuous Integration environment.
### Operators: Artifact Migration
As an operator of Connect, if you discover incompatible plugins, there are multiple ways to resolve the incompatibility. They are listed below from most to least preferable.
  1. Check the latest release from your plugin provider, and if it is compatible, upgrade.
```

**How to spot**
When reviewing docs/kafka-connect code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20239: document kafka acls required for connect (#21615).

**Test**
No test added.
