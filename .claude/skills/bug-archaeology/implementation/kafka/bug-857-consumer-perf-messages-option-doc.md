**Commit**: 7738db9b2d · **Ticket**: KAFKA-19533

**Subsystem**: Tools / CLI documentation

**Category**: Misleading/wrong documentation or log message

**Root cause**
The `--messages` option in Kafka performance tool CLI had a help text that read "The number of messages to send or consume." Consumer performance tools only consume; they never send. The misleading "send or consume" phrasing could confuse users into thinking the tool also supports producing, and obscured what the flag actually controls for consumer-only tools.

**Before/After**
```
// Before:
--messages   The number of messages to send or consume.

// After:
--messages   The number of messages to consume.
```

**How to spot**
Review help strings for options shared between producer and consumer performance tools; copy-paste from the producer variant often leaves "send or" in consumer-only descriptions. A simple grep for "send or consume" in CLI option descriptions surfaces these.

**Test**
CLI help-text tests (or manual inspection of `--help` output) confirm the corrected description appears for consumer performance tool invocations.
