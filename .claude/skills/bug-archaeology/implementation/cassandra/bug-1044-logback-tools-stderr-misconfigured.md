# Bug 1044: logback-tools.xml Appender Ignores System.err Target

**Commit**: dc73c0b31f · **Ticket**: CASSANDRA-9937
**Subsystem**: Logging / configuration
**Category**: Wrong constant / default value

---

## Root Cause
The `logback-tools.xml` appender used the Logback `ConsoleAppender` with the `target` attribute set on the `<appender>` XML element itself (`target="System.err"`), which is not a valid attribute in that position. Logback ignores unrecognised XML attributes silently, so the appender defaulted to `System.out` instead of `System.err`. The fix moves `<target>System.err</target>` inside the appender element as a child element, which is the correct Logback configuration syntax.

### Before / After
```xml
<!-- BEFORE — target attribute on appender element is silently ignored -->
<appender name="STDERR" target="System.err" class="ch.qos.logback.core.ConsoleAppender">
  <encoder>...</encoder>
</appender>

<!-- AFTER — target as child element, correctly recognized by Logback -->
<appender name="STDERR" class="ch.qos.logback.core.ConsoleAppender">
  <target>System.err</target>
  <encoder>...</encoder>
</appender>
```

---

## How to Spot
Logback's XML configuration is silently permissive: unknown attributes or misplaced elements are ignored without any warning. Whenever a Logback appender is supposed to write to `System.err`, verify that `<target>System.err</target>` appears as a child element of `<appender>`, not as an XML attribute. A quick smoke test — running a Cassandra tool and checking that error output goes to stderr rather than stdout — would surface this class of misconfiguration.

---

## Test
No test added.
