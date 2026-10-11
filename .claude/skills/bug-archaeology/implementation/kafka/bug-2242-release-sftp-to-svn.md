**Commit**: fa936e7e08 · **Ticket**: KAFKA-17735
**Subsystem**: release (release.py, sftp.py → svn.py)
**Category**: Logic error in condition

**Root cause**
The release script uploaded artifacts via SFTP to `home.apache.org`, which the ASF deprecated in favour of SVN commits to `dist.apache.org/repos/dist/dev/kafka`. The old SFTP path silently failed or was unreachable, blocking the release process. Additionally, `runtime.cmd()` returned `None` instead of a boolean, causing callers that checked the return value to always treat the result as falsy.

**Before/After**
```python
# Before – SFTP upload (broken path)
sftp.put(local_path, remote_path)
# runtime.cmd returned None

# After – SVN commit
svn.add(staged_dir)
svn.commit("Staging Kafka release ...")
# runtime.cmd returns True/False
```

**How to spot**
Release scripts that use deprecated upload mechanisms will fail silently or with confusing SSH errors; verify the upload target matches current ASF infrastructure documentation and that command wrappers return explicit boolean results so callers can detect failure.

**Test**
No test added.
