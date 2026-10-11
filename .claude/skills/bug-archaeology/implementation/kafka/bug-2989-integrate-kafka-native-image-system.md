**Commit**: bb6a042e99 · **Ticket**: KAFKA-16827
**Subsystem**: .gitignore
**Category**: Logic error in condition

**Root cause**
This PR does following things

System tests should bring up Kafka broker in the native mode
System tests should run on Kafka broker in native mode
Extract out native build command so that it can be reused. Allow system tests to run on Native Kafka broker using Docker mechanism

To run system tests by bringing up Kafka in native mode:
Pass kafka_mode as native in the ducktape globals:--globals '{\"kafka_mode\":\"native\"}'

Running system tests by bringing up kafka in native mode via docker mechanism
_DUCKTAPE_OPTIONS="--globals '{\"kafka_mode\":\"native\"}'" TC_PATHS="tests/kafkatest/tests/"  bash tests/docker/run_tests.sh

To only bring up ducker nodes to cater native kafka
bash tests/docke...

**Before/After**
```java
// Before
  KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false  -Dcom.sun.management.jmxremote.ssl=false "
# Launch mode
if [ "x$DAEMON_MODE" = "xtrue" ]; then
  nohup "$JAVA" $KAFKA_HEAP_OPTS $KAFKA_JVM_PERFORMANCE_OPTS $KAFKA_GC_LOG_OPTS $KAFKA_JMX_OPTS $KAFKA_LOG4J_CMD_OPTS -cp "$CLASSPATH" $KAFKA_OPTS "$@" > "$CONSOLE_OUTPUT_FILE" 2>&1 < /dev/null &
  exec "$JAVA" $KAFKA_HEAP_OPTS $KAFKA_JVM_PERFORMANCE_OPTS $KAFKA_GC_LOG_OPTS $KAFKA_JMX_OPTS $KAFKA_LOG4J_CMD_OPTS -cp "$CLASSPATH" $KAFKA_OPTS "$@"
import kafka.utils.Exit
object KafkaDockerWrapper {
COPY native-image-configs native-image-configs
RUN mkdir kafka; \
    wget -nv -O kafka.tgz.asc "$kafka_url.asc"; \
    tar xfz kafka.tgz -C kafka --strip-components 1; \
    cd kafka ;  \

// After
kafka.Kafka
  KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.authenticate=false  -Dcom.sun.management.jmxremote.ssl=false "
# If KAFKA_MODE=native, it will bring up Kafka in the native mode.
# It expects the Kafka executable binary to be present at $base_dir/kafka.Kafka.
# This is specifically used to run system tests on native Kafka - by bringing up Kafka in the native mode.
if [[ "x$KAFKA_MODE" == "xnative" ]] && [[ "$*" == *"kafka.Kafka"* ]]; then
  exec $base_dir/kafka.Kafka start --config "$2"  $KAFKA_LOG4J_CMD_OPTS $KAFKA_JMX_OPTS $KAFKA_OPTS
  # Launch mode
  if [ "x$DAEMON_MODE" = "xtrue" ]; then
    nohup "$JAVA" $KAFKA_HEAP_OPTS $KAFKA_JVM_PERFORMANCE_OPTS $KAFKA_GC_LOG_OPTS $KAFKA_JMX_OPTS $KAFKA_LOG4J_CMD_OPTS -cp "$CLASSPATH" $KAFKA_OPTS "$@" > "$CONSOLE_OUTPUT_FILE" 2>&1 < /dev/null &
  else
    exec "$JAVA" $KAFKA_HEAP_OPTS $KAFKA_JVM_PERFORMANCE_OPTS $KAFKA_GC_LOG_OPTS $KAFKA_JMX_OPTS $KAFKA_LOG4J_CMD_OPTS -cp "$CLASSPATH" $KAFKA_OPTS "$@"
```

**How to spot**
When reviewing .gitignore code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16827: integrate kafka native-image with system tests (#16046).

**Test**
Test modified in Dockerfile covering the corrected behaviour.
