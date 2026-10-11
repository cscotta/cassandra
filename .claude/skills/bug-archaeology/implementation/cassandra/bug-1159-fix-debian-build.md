# Bug 1159: Debian Package References Non-Existent Binary sstablekeys

**Commit**: b44f409885 · **Ticket**: n/a
**Subsystem**: debian/cassandra.install
**Category**: State not cleaned up

---

## Root Cause

The Debian main package manifest `cassandra.install` referenced `bin/sstablekeys`, a binary that had been renamed to `bin/sstablelister` but whose manifest entry was never updated. When the Debian package was built, `dpkg-buildpackage` would fail to locate `bin/sstablekeys` in the build output and abort. The fix is a one-line rename of the manifest entry to match the actual binary name.

### Before / After

```
# BEFORE
bin/sstablekeys usr/bin
```

```
# AFTER
bin/sstablelister usr/bin
```

---

## How to Spot

Any commit that renames a tool or binary must also update all packaging manifests. Because Debian `.install` files are plain text and not validated at code-review time, renamed entries silently linger until a package build is attempted. The pattern to watch for is a refactoring commit that renames a file in `bin/` without a corresponding change to `debian/*.install`; a grep for the old name across the packaging directory would catch this immediately.

---

## Test

No test added.
