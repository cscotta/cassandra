**Commit**: 6bc7be70d7 · **Ticket**: KAFKA-17922
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
             Admin adminClient = clusterInstance.createAdminClient()) {
        Properties config = new Properties();
        try (Admin adminClient = clusterInstance.createAdminClient(config)) {
             Admin adminClient = clusterInstance.createAdminClient()) {
        try (Admin client = clusterInstance.createAdminClient()) {
        try (Admin client = cluster.createAdminClient()) {
        try (Admin client = cluster.createAdminClient()) {
        try (Admin client = cluster.createAdminClient()) {
        try (Admin client = cluster.createAdminClient()) {

// After
import java.util.HashMap;
import java.util.Map;
             Admin adminClient = clusterInstance.admin()) {
        Map<String, Object> config = new HashMap<>();
        try (Admin adminClient = clusterInstance.admin(config)) {
             Admin adminClient = clusterInstance.admin()) {
        try (Admin client = clusterInstance.admin()) {
        try (Admin client = cluster.admin()) {
        try (Admin client = cluster.admin()) {
        try (Admin client = cluster.admin()) {
        try (Admin client = cluster.admin()) {
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17922 add helper to clusterinstance to create client component  (#17666).

**Test**
Test modified in AdminFenceProducersTest.java covering the corrected behaviour.
