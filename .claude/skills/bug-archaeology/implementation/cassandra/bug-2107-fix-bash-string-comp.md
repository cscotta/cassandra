# Bug 2107: Incorrect JVM Version Check Skips UseCondCardMark on Java 1.7

**Commit**: 0820b20fb2 · **Ticket**: n/a
**Subsystem**: conf/cassandra-env.sh
**Category**: Wrong constant / default value

---

## Root Cause

The startup script checked `$JVM_VERSION \> "1.6.0"` to decide whether to enable the `-XX:+UseCondCardMark` JVM flag, which is only valid on JDK 1.7+. Because bash lexicographic comparison evaluates `"1.7.x" > "1.6.0"` as true, the flag was also applied to JDK 1.6 runtimes, which don't support it. The correct threshold is `"1.7"` — bash compares `"1.7.x" > "1.7"` as true (since any patch suffix makes the version string longer), so this reliably gates the flag to 1.7 and above only.

### Before / After

```bash
# BEFORE
if [ "$JVM_VERSION" \> "1.6.0" ]; then
    JVM_OPTS="$JVM_OPTS -XX:+UseCondCardMark"
fi
```

```bash
# AFTER
# note: bash evals '1.7.x' as > '1.7' so this is really a >= 1.7 jvm check
if [ "$JVM_VERSION" \> "1.7" ] ; then
    JVM_OPTS="$JVM_OPTS -XX:+UseCondCardMark"
fi
```

---

## How to Spot

Version threshold comparisons in shell scripts using lexicographic string comparison are inherently fragile. Verify that the chosen comparison string correctly separates all valid version strings on both sides of the threshold, paying attention to how dotted version strings sort lexicographically.

---

## Test

No test added.
