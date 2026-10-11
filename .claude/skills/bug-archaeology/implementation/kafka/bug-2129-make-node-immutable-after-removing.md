**Commit**: 7366f0487a · **Ticket**: KAFKA-17128
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  private[server] var _brokerId: Int = getInt(ServerConfigs.BROKER_ID_CONFIG)
  def brokerId: Int = _brokerId
  private[server] var _nodeId: Int = getInt(KRaftConfigs.NODE_ID_CONFIG)
  def nodeId: Int = _nodeId
        config._brokerId = getOrGenerateBrokerId(initialMetaPropsEnsemble)
        config._nodeId = config.brokerId

// After
  var brokerId: Int = getInt(ServerConfigs.BROKER_ID_CONFIG)
  val nodeId: Int = getInt(KRaftConfigs.NODE_ID_CONFIG)
        config.brokerId = getOrGenerateBrokerId(initialMetaPropsEnsemble)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17128 make node.id immutable after removing zookeeper migration (#17616).

**Test**
No test added.
