**Commit**: 19b655fa99 · **Ticket**: KAFKA-19715

**Subsystem**: .github/workflows/docker_promote.yml

**Category**: Wrong constant/default value

**Root cause**
The GitHub Actions workflow for Docker promotion used `docker/setup-qemu-action@v3.6`, which was outdated. Keeping third-party GitHub Actions pinned to stale versions exposes the CI pipeline to known security vulnerabilities and bugs in the action itself. The fix bumps the pin from `v3.6` to `v3.7` in both `docker_promote.yml` and `docker_rc_release.yml`.

**Before**
```yaml
# Before
- uses: docker/setup-qemu-action@v3.6
```

**After**
```yaml
# After
- uses: docker/setup-qemu-action@v3.7
```

**How to spot**
Regularly audit `.github/workflows/*.yml` for third-party `uses:` pins that lag behind the current release; stale action versions may contain security issues or incompatibilities with the latest GitHub Actions runner.

**Test**
Trigger a Docker promotion workflow run after the version bump and confirm it completes successfully without errors introduced by the updated action.
