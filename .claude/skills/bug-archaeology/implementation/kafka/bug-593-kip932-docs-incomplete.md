**Commit**: 56c46f8315 · **Ticket**: KAFKA-19364

**Subsystem**: docs/api.html, docs/design.html

**Category**: Missing null/bounds check

**Root cause**
The KIP-932 (Share Groups) documentation in `docs/api.html` and `docs/design.html` was incomplete or out of date with respect to the API version status and the detailed design sections for share-partition state management. Without accurate documentation, operators and integrators reading the public docs would be working from stale or partial specifications. The fix updates both files with current API descriptions, corrects version references, and expands the design section to reflect the final KIP-932 implementation.

**Before**
```html
<!-- Before: share group API entries missing or incomplete in api.html -->
```

**After**
```html
<!-- After: share group API entries updated, design.html expanded -->
```

**How to spot**
Compare the official KIP-932 proposal against `docs/api.html` for each new API (ShareFetch, ShareAcknowledge, etc.); any API that appears in the protocol spec but is absent or mis-described in the docs page is a documentation gap.

**Test**
Review the rendered HTML pages and confirm all KIP-932 API entries are present, versioned correctly, and link to the appropriate design-doc sections.
