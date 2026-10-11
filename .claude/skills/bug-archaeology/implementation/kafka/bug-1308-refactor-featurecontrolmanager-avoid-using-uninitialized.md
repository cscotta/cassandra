**Commit**: d171ff08a7 · **Ticket**: KAFKA-18858
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
The `FeatureControlManager` used `MetadataVersion#LATEST_PRODUCTION` as uninitialized MV. This makes other component may get a stale MV. In production code, the `FeatureControlManager` set MV when replaying `FeatureLevelRecord`, so we can set `Optional.empty()` as uninitialized MV. If other components get an empty result, the `FeatureLevelRecord` throws an exception like `FeaturesImage`.

**Before/After**
```java
// Before
     * If the log is empty, write the bootstrap records. If the log is not empty, do some validation and
        boolean isEmpty,
        MetadataVersion curMetadataVersion,
        if (isEmpty) {
                    curMetadataVersion);
        if (featureControl.metadataVersion().isDirectoryAssignmentSupported()) {
        if (featureControl.metadataVersion().isDirectoryAssignmentSupported()) {
        records.add(new ApiMessageAndVersion(record, featureControl.metadataVersion().
        if (!featureControl.metadataVersion().isControllerRegistrationSupported()) {
        if (!featureControl.metadataVersion().isControllerRegistrationSupported()) {

// After
import java.util.Optional;
     * If the metadata version is empty, write the bootstrap records. If the metadata version is not empty, do some validation and
        Optional<MetadataVersion> curMetadataVersion,
        if (curMetadataVersion.isEmpty()) {
                    curMetadataVersion.get());
        if (featureControl.metadataVersionOrThrow().isDirectoryAssignmentSupported()) {
        if (featureControl.metadataVersionOrThrow().isDirectoryAssignmentSupported()) {
        records.add(new ApiMessageAndVersion(record, featureControl.metadataVersionOrThrow().
        if (!featureControl.metadataVersionOrThrow().isControllerRegistrationSupported()) {
        if (!featureControl.metadataVersionOrThrow().isControllerRegistrationSupported()) {
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18858 refactor featurecontrolmanager to avoid using uninitialized mv (#19040).

**Test**
Test modified in ClusterControlManagerTest.java covering the corrected behaviour.
