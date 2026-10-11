**Commit**: 90ff97b51d · **Ticket**: KAFKA-18094
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * Create an instance with the specified parameters.
     *
     * @param name The topic name
     * @param internal Whether the topic is internal to Kafka
     * @deprecated Since 3.0 use {@link #TopicListing(String, Uuid, boolean)} instead
     */
    @Deprecated
    public TopicListing(String name, boolean internal) {
        this.name = name;
        this.internal = internal;
        this.topicId = Uuid.ZERO_UUID;

// After
                        <li>The <code>org.apache.kafka.clients.admin.TopicListing.TopicListing(String, boolean)</code> method were removed.
                            Please use <code>org.apache.kafka.clients.admin.TopicListing.TopicListing(String, Uuid, boolean)</code> instead.
                        </li>
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18094 remove deprecated topiclisting(string, boolean) (#18248).

**Test**
No test added.
