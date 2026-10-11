# Bug 0760: Stress daemon help shows wrong flag name

**Commit**: 3f41d7a76f · **Ticket**: CASSANDRA-12563
**Subsystem**: cassandra-stress / CLI
**Category**: Wrong constant / default value

---

## Root Cause
The `sendToDaemonHelpPrinter()` method in `SettingsMisc` printed `"-sendToDaemon <host>"` in its usage string, but the actual CLI flag recognized by the parser was `-sendto`. Likewise the error message for an invalid specifier referred to `-send-to`. Users who copied the help text verbatim would get a parse failure with no clear explanation.

### Before / After
```java
// BEFORE
System.out.println("Usage: -sendToDaemon <host>");
...
System.out.println("Invalid -send-to specifier: " + Arrays.toString(params));

// AFTER
System.out.println("Usage: -sendto <host>");
...
System.out.println("Invalid -sendto specifier: " + Arrays.toString(params));
```

---

## How to Spot
Any time a help string or error message hard-codes an option name, compare it character-for-character against the string the argument parser actually matches. A mismatch is invisible at compile time; the only symptom is user confusion or a silent parse failure when someone copies the example.

---

## Test
No test added.
