**Commit**: 66b3c07954 · **Ticket**: KAFKA-19576
**Subsystem**: config/log4j2.yaml
**Category**: Wrong constant / default value

**Root cause**
The Log4j2 rolling-file appender for the state-change log had a typo in its `filePattern` attribute: `stage-change.log.%d{yyyy-MM-dd-HH}` instead of `state-change.log.%d{yyyy-MM-dd-HH}`. Every time the appender rolled the active log file, the rotated file was written with the name `stage-change.log.[date]` instead of `state-change.log.[date]`. This made the rotated files invisible to tools and monitoring pipelines that searched for `state-change.log.*`, and could cause confusion when diagnosing state-change issues from historical logs.

**Before/After**
```yaml
# Before: typo "stage" instead of "state"
filePattern: "${sys:kafka.logs.dir}/stage-change.log.%d{yyyy-MM-dd-HH}"

# After: correct filename pattern
filePattern: "${sys:kafka.logs.dir}/state-change.log.%d{yyyy-MM-dd-HH}"
```

**How to spot**
Compare the `fileName` and `filePattern` attributes on every Log4j2 rolling appender — both should reference the same log file base name. A mismatch between the two, or a simple typographic difference, will silently produce misnamed rotated files that are invisible to log rotation tooling and monitoring scripts.

**Test**
No test added. This is a configuration-only fix. The upgrade notes document the change so that operators who have monitoring or log-rotation rules targeting `stage-change.log.*` can update them.
