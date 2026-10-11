**Commit**: d2ad418cfd · **Ticket**: KAFKA-18156
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                                                        + "and <code>group.max.session.timeout.ms</code>.";
 * `--session-timeout <n>`
                 static_membership=False, max_messages=-1, session_timeout_sec=30, enable_autocommit=False,
        self.group_protocol = group_protocol
        self.group_remote_assignor = group_remote_assignor
        cmd += " --reset-policy %s --group-id %s --topic %s --session-timeout %s" % \
               (self.reset_policy, self.group_id, self.topic,
                self.session_timeout_sec*1000)

                       timeout_sec=self.session_timeout_sec+5,
                           timeout_sec=self.session_timeout_sec+5,

// After
                                                        + "and <code>group.max.session.timeout.ms</code>. Note that this configuration is not supported when <code>group.protocol</code> "
                                                        + "is set to \"consumer\".";
 * `--session-timeout <n>` - note that this configuration is not supported when group protocol is consumer
                 static_membership=False, max_messages=-1, session_timeout_sec=0, enable_autocommit=False,
        cmd += " --reset-policy %s --group-id %s --topic %s" % \
                (self.reset_policy, self.group_id, self.topic)

        if self.session_timeout_sec > 0:
            cmd += " --session-timeout %s" % self.session_timeout_sec*1000

                       timeout_sec=60,
                           timeout_sec=60,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18156 verifiableconsumer should ignore "--session-timeout" when using consumer protocol (#18036).

**Test**
Test modified in verifiable_client.py covering the corrected behaviour.
