**Commit**: b9ccab42fe · **Ticket**: KAFKA-18472
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.MetadataSupport;
    private MetadataSupport metadataSupport = null;
    public KafkaApisBuilder setMetadataSupport(MetadataSupport metadataSupport) {
        this.metadataSupport = metadataSupport;
        if (metadataSupport == null) throw new RuntimeException("you must set metadataSupport");
                             metadataSupport,
      // Create the request processor objects.
      val raftSupport = RaftSupport(forwardingManager, metadataCache)
        metadataSupport = raftSupport,
                val metadataSupport: MetadataSupport,
    metadataSupport.forward(request, responseCallback)
      metadataSupport.forwardingManager.get.forwardRequest(request,

// After
import kafka.server.ForwardingManager;
    private ForwardingManager forwardingManager = null;
    public KafkaApisBuilder setForwardingManager(ForwardingManager forwardingManager) {
        this.forwardingManager = forwardingManager;
        if (forwardingManager == null) throw new RuntimeException("you must set forwardingManager");
                             forwardingManager,
        forwardingManager = forwardingManager,
                val forwardingManager: ForwardingManager,
    forwardingManager.forwardRequest(request, responseCallback)
      forwardingManager.forwardRequest(request,
      forwardingManager.forwardRequest(request,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18472: remove metadatasupport (#18483).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
