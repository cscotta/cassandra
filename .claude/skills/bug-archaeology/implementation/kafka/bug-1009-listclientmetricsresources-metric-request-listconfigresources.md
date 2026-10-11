**Commit**: 78ea8782ae · **Ticket**: KAFKA-18904
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Before 4.1, the api key 74 is `ListClientMetricsResources`. After 4.1,
it's `ListConfigResources`. If users sent a v0 ListConfigResources to
broker, the metric doesn't record request with
`ListClientMetricsResources`. This PR is to add
`ListClientMetricsResources` metric if the request is v0
`ListConfigResources`.

**Before/After**
```java
// Before
    val request = buildRequest(new ListConfigResourcesRequest.Builder(
      new ListConfigResourcesRequestData().setResourceTypes(util.List.of(ConfigResource.Type.CLIENT_METRICS.id))).build(0))
    metadataCache = mock(classOf[KRaftMetadataCache])
    val resources = util.Set.of("client-metric1", "client-metric2")
    when(clientMetricsManager.listClientMetricsResources).thenReturn(resources)
    kafkaApis = createKafkaApis()
    kafkaApis.handle(request, RequestLocal.noCaching)
    val response = verifyNoThrottling[ListConfigResourcesResponse](request)
    val expectedResponseData = new ListConfigResourcesResponseData()
    assertEquals(expectedResponseData, response.data)
    verify(metadataCache, never).getAllTopics
    verify(groupConfigManager, never).groupIds

// After
        } else if (header.apiKey == ApiKeys.LIST_CONFIG_RESOURCES && header.apiVersion == 0) {
          Seq(RequestMetrics.LIST_CLIENT_METRICS_RESOURCES_METRIC_NAME, header.apiKey.name)
    val requestMetrics = new RequestChannelMetrics(util.Set.of(ApiKeys.LIST_CONFIG_RESOURCES))
    try {
      val request = buildRequest(new ListConfigResourcesRequest.Builder(
        new ListConfigResourcesRequestData().setResourceTypes(util.List.of(ConfigResource.Type.CLIENT_METRICS.id))).build(0),
        requestMetrics = requestMetrics)
      metadataCache = mock(classOf[KRaftMetadataCache])
      val resources = util.Set.of("client-metric1", "client-metric2")
      when(clientMetricsManager.listClientMetricsResources).thenReturn(resources)
      kafkaApis = createKafkaApis()
      kafkaApis.handle(request, RequestLocal.noCaching)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18904: [4/n] add listclientmetricsresources metric if request is v0 listconfigresources (#19877).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
