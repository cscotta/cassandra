**Commit**: 10873e4210 · **Ticket**: KAFKA-18281
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
When a cluster is configured with a dynamic controller quorum, KRaft replica's endpoint are computed using the advertised.listeners property and not the quorum.controller.voters property. This change in the configuration makes it difficult to keeping all previous node configurations compatible with the new endpoint discovery functionality. The least intrusive solution is to rely on Kafka's reverse hostname lookup when the hostname is not specified. The effective advertised controller listener now remove '0.0.0.0' hostname if the endpoint came from the listener configuration and not the advertised.listener configuration.

**Before/After**
```java
// Before
    val controllerAdvertisedListeners = advertisedListeners.filter(l => controllerListenerNames.contains(l.listenerName.value()))
        .orElse(controllerListenersValue.find(endpoint => endpoint.listenerName.equals(ListenerName.normalised(name))))
    // Only expose broker listeners
    advertisedListeners.filterNot(l => controllerListenerNames.contains(l.listenerName.value()))
  }

  // Use advertised listeners if defined, fallback to listeners otherwise
  private def advertisedListeners: Seq[EndPoint] = {
    if (advertisedListenersProp != null) {

// After
    val advertisedListenersProp = getString(SocketServerConfigs.ADVERTISED_LISTENERS_CONFIG)
    val controllerAdvertisedListeners = if (advertisedListenersProp != null) {
      CoreUtils.listenerListToEndPoints(advertisedListenersProp, effectiveListenerSecurityProtocolMap, requireDistinctPorts=false)
        .filter(l => controllerListenerNames.contains(l.listenerName.value()))
    } else {
      Seq.empty
    }
        .orElse(
          // If users don't define advertised.listeners, the advertised controller listeners inherit from listeners configuration
          // which match listener names in controller.listener.names.
          // Removing "0.0.0.0" host to avoid validation errors. This is to be compatible with the old behavior before 3.9.
          // The null or "" host does a reverse lookup in ListenerInfo#withWildcardHostnamesResolved.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18281: kafka is improperly validating non-advertised listeners for routable controller addresses (#18387).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
