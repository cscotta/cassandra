**Commit**: 2486cf1abe · **Ticket**: KAFKA-19724
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Kafka Streams does not catch Error types that occur during
`GlobalStreamThread` initiation, and therefore it is not possible to
trace the error (for example, an `ExceptionInInitializerError` occurs
when RocksDB is not found for a global store). This is because errors
are not caught and logged. The catch block in `GlobalStreamThread#initialize()` has been ensured to
catch `Throwable` instead of `Exception`. Additionally, the empty
`setUncaughtHandler` set operation that prevented this from taking
effect when users employed setUncaughtExceptionHandler has been removed.

**Before/After**
```java
// Before

                if (globalStreamThread != null) {
                    globalStreamThread.setUncaughtExceptionHandler((t, e) -> { }
                    );
                }
         * @throws IllegalStateException If store gets registered after initialized is already finished
        } catch (final Exception fatalException) {
        final HashMap<String, Object> properties = new HashMap<>();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "blah");
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "testAppId");
        properties.put(StreamsConfig.STATE_DIR_CONFIG, baseDirectoryName);
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class.getName());

// After
         * @throws IllegalStateException If a store gets registered after initialized is already finished
        } catch (final Throwable fatalException) {
        final HashMap<String, Object> properties = getStreamProperties();
    @Test
    public void shouldThrowStreamsExceptionOnStartupIfThrowableOccurred() throws Exception {
        final String exceptionMessage = "Throwable occurred!";
        final MockConsumer<byte[], byte[]> consumer = new MockConsumer<>(AutoOffsetResetStrategy.EARLIEST.name()) {
            @Override
            public List<PartitionInfo> partitionsFor(final String topic) {
                throw new ExceptionInInitializerError(exceptionMessage);
            }
        };
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19724: global stream thread should not ignore any exceptions (#20668).

**Test**
Test modified in GlobalStreamThreadTest.java covering the corrected behaviour.
