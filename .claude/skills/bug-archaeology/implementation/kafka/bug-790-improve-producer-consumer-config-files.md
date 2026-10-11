**Commit**: 2ba30cc466 · **Ticket**: KAFKA-19574
**Subsystem**: config/consumer.properties
**Category**: Logic error in condition

**Root cause**
This is an attempt at improving the client configuration files. We now
have sections and comments similar to the other properties files. Reviewers: Kirk True <ktrue@confluent.io>, Luke Chen <showuon@gmail.com>

---------

Signed-off-by: Federico Valeri <fedevaleri@gmail.com>

**Before/After**
```java
// Before
#
#
# see org.apache.kafka.clients.consumer.ConsumerConfig for more details
# list of brokers used for bootstrapping knowledge about the rest of the cluster
# format: host1:port1,host2:port2 ...

// After
import java.io.FileInputStream;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.fail;

    /**
     * Validates config/consumer.properties file to avoid getting out of sync with ConsumerConfig.
     */
    @Test
    public void testValidateConfigPropertiesFile() {
        Properties props = new Properties();

        try (InputStream inputStream = new FileInputStream(System.getProperty("user.dir") + "/../config/consumer.properties")) {
```

**How to spot**
When reviewing config/consumer.properties code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19574: improve producer and consumer config files (#20302).

**Test**
Test modified in ConsumerConfigTest.java covering the corrected behaviour.
