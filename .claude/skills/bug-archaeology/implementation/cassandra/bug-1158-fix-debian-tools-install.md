# Bug 1158: Debian Tools Package Installs Removed Binaries and Omits New One

**Commit**: b090ed6938 · **Ticket**: n/a
**Subsystem**: debian/cassandra-tools.install
**Category**: State not cleaned up

---

## Root Cause

The Debian package manifest `cassandra-tools.install` still listed `tools/bin/json2sstable` and `tools/bin/sstable2json`, which were removed from the codebase, while omitting the newly added `tools/bin/sstableofflinerelevel`. When `dpkg-buildpackage` tried to install files listed in the manifest, it would fail because the removed binaries no longer existed in the build output. The underlying issue is that the manifest was not updated in sync with the binary additions and removals, a classic packaging drift problem.

### Before / After

```
# BEFORE
tools/bin/json2sstable usr/bin
tools/bin/sstable2json usr/bin
tools/bin/sstablelevelreset usr/bin
tools/bin/sstablemetadata usr/bin
tools/bin/sstablerepairedset usr/bin
```

```
# AFTER
tools/bin/sstableofflinerelevel usr/bin
tools/bin/sstablelevelreset usr/bin
tools/bin/sstablemetadata usr/bin
tools/bin/sstablerepairedset usr/bin
```

---

## How to Spot

Packaging manifests (`.install`, `MANIFEST.in`, spec file `%files` sections) are easy to forget when tools are renamed or removed. A reviewer touching any commit that adds, removes, or renames a binary or script should always cross-check every packaging manifest in the repository. CI that builds the Debian or RPM package on every commit would have caught this immediately; its absence allowed the drift to persist.

---

## Test

No test added.
