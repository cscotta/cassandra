# Bug 0211: NTS Log Message Fires Before Option Validation, Hiding Unrecognised Options

**Commit**: 8b331ddb4d · **Ticket**: CASSANDRA-18679
**Subsystem**: src/java/org/apache/cassandra/locator/NetworkTopologyStrategy.java
**Category**: Logic error in condition

---

## Root Cause

`NetworkTopologyStrategy` logged the "Configured datacenter replicas are …"
message inside the constructor, before `validateExpectedOptions()` had run.  If
an unrecognised strategy option was passed, the log line would include it in the
datacenter map — since the constructor had already processed it — and validation
would then silently reject or warn about the option without the log ever having
correctly reflected the final validated state.  Moving the log statement to the
end of `validateExpectedOptions()` (which calls `super.validateExpectedOptions()`
first) ensures the message only appears after all options have been validated and
the resulting map is definitively correct.

### Before / After

```java
// BEFORE – log inside constructor, before validation
datacenters = Collections.unmodifiableMap(newDatacenters);
aggregateRf = ReplicationFactor.withTransient(replicas, trans);
logger.info("Configured datacenter replicas are {}", FBUtilities.toString(datacenters));
```

```java
// AFTER – log at end of validateExpectedOptions(), after super validates
super.validateExpectedOptions();
logger.info("Configured datacenter replicas are {}", FBUtilities.toString(datacenters));
```

---

## How to Spot

Log statements that describe "the final configured state" should appear after all
validation and normalisation steps have completed, not in the constructor.  When
reviewing initialisation code, check whether log messages are placed before or
after validation calls, and whether the logged data could differ from the
post-validation state.

---

## Test

No test added.
