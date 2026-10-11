# Bug 2063: Duplicate-IP Error Message Prints Wrong Values

**Commit**: abbb8601ee · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/locator/YamlFileNetworkTopologySnitch.java
**Category**: Wrong constant / default value

---

## Root Cause

`YamlFileNetworkTopologySnitch` detects duplicate IP addresses in the topology config file and throws a `ConfigurationException` with a `String.format` call. The format string contained one `%s` placeholder (for the duplicate IP), but the `format` arguments passed were `datacenter.dc_name` and `rack.rack_name` — two entirely unrelated strings. Java's `String.format` silently ignores extra arguments and substitutes the first one for `%s`, so the error message showed the datacenter or rack name instead of the offending IP address. The fix passes the correct variable (`endpoint` or `dcLocalAddress`) as the sole format argument.

### Before / After

```java
// BEFORE
throw new ConfigurationException(String.format(
    "IP address '%s' appears more than once in the topology configuration file",
    datacenter.dc_name, rack.rack_name)); // wrong args
```

```java
// AFTER
throw new ConfigurationException(String.format(
    "IP address '%s' appears more than once in the topology configuration file",
    endpoint)); // correct arg
```

---

## How to Spot

When calling `String.format`, verify that the number and type of arguments match the placeholders in the format string. A mismatch where extra arguments are passed goes undetected at compile time and at runtime — only the error message content reveals the problem. Static analysis tools (e.g., SpotBugs `VA_FORMAT_STRING_EXTRA_ARGUMENTS_PASSED`) catch this class of bug.

---

## Test

No test added.
