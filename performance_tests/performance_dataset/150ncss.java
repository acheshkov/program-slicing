private HttpRequest prepareHttpMessage(
    String activityId,
    Uri physicalAddress,
    ResourceOperation resourceOperation,
    RxDocumentServiceRequest request) throws Exception {
    HttpRequest httpRequestMessage;
    String requestUri;
    HttpMethod method;
    switch (resourceOperation.operationType) {
    case Create:
        requestUri = getResourceFeedUri(resourceOperation.resourceType, physicalAddress.getURIAsString(), request);
        method = HttpMethod.POST;
        assert request.getContentAsByteBufFlux() != null;
        httpRequestMessage = new HttpRequest(method, requestUri, physicalAddress.getURI().getPort());
        httpRequestMessage.withBody(request.getContentAsByteBufFlux());
        break;
    case ExecuteJavaScript:
        requestUri = getResourceEntryUri(resourceOperation.resourceType, physicalAddress.getURIAsString(), request);
        method = HttpMethod.POST;
        assert request.getContentAsByteBufFlux() != null;
        httpRequestMessage = new HttpRequest(method, requestUri.toString(), physicalAddress.getURI().getPort());
        httpRequestMessage.withBody(request.getContentAsByteBufFlux());
        break;
    case Delete:
        requestUri = getResourceEntryUri(resourceOperation.resourceType, physicalAddress.getURIAsString(), request);
        method = HttpMethod.DELETE;
        httpRequestMessage = new HttpRequest(method, requestUri.toString(), physicalAddress.getURI().getPort());
        break;
    case Read:
        requestUri = getResourceEntryUri(resourceOperation.resourceType, physicalAddress.getURIAsString(), request);
        method = HttpMethod.GET;
        httpRequestMessage = new HttpRequest(method, requestUri.toString(), physicalAddress.getURI().getPort());
        break;
    case ReadFeed:
        requestUri = getResourceFeedUri(resourceOperation.resourceType, physicalAddress.getURIAsString(), request);
        method = HttpMethod.GET;
        httpRequestMessage = new HttpRequest(method, requestUri.toString(), physicalAddress.getURI().getPort());
        break;
    case Replace:
        requestUri = getResourceEntryUri(resourceOperation.resourceType, physicalAddress.getURIAsString(), request);
        method = HttpMethod.PUT;
        assert request.getContentAsByteBufFlux() != null;
        httpRequestMessage = new HttpRequest(method, requestUri.toString(), physicalAddress.getURI().getPort());
        httpRequestMessage.withBody(request.getContentAsByteBufFlux());
        break;
    case Update:
        requestUri = getResourceEntryUri(resourceOperation.resourceType, physicalAddress.getURIAsString(), request);
        method = new HttpMethod("PATCH");
        assert request.getContentAsByteBufFlux() != null;
        httpRequestMessage = new HttpRequest(method, requestUri.toString(), physicalAddress.getURI().getPort());
        httpRequestMessage.withBody(request.getContentAsByteBufFlux());
        break;
    case Query:
    case SqlQuery:
        requestUri = getResourceFeedUri(resourceOperation.resourceType, physicalAddress.getURIAsString(), request);
        method = HttpMethod.POST;
        assert request.getContentAsByteBufFlux() != null;
        httpRequestMessage = new HttpRequest(method, requestUri.toString(), physicalAddress.getURI().getPort());
        httpRequestMessage.withBody(request.getContentAsByteBufFlux());
        HttpTransportClient.addHeader(httpRequestMessage.headers(), HttpConstants.HttpHeaders.CONTENT_TYPE, request);
        break;
    case Upsert:
        requestUri = getResourceFeedUri(resourceOperation.resourceType, physicalAddress.getURIAsString(), request);
        method = HttpMethod.POST;
        assert request.getContentAsByteBufFlux() != null;
        httpRequestMessage = new HttpRequest(method, requestUri.toString(), physicalAddress.getURI().getPort());
        httpRequestMessage.withBody(request.getContentAsByteBufFlux());
        break;
    case Head:
        requestUri = getResourceEntryUri(resourceOperation.resourceType, physicalAddress.getURIAsString(), request);
        method = HttpMethod.HEAD;
        httpRequestMessage = new HttpRequest(method, requestUri.toString(), physicalAddress.getURI().getPort());
        break;
    case HeadFeed:
        requestUri = getResourceFeedUri(resourceOperation.resourceType, physicalAddress.getURIAsString(), request);
        method = HttpMethod.HEAD;
        httpRequestMessage = new HttpRequest(method, requestUri.toString(), physicalAddress.getURI().getPort());
        break;
    default:
        assert false : "Unsupported operation type";
        throw new IllegalStateException();
    }
    Map<String, String> documentServiceRequestHeaders = request.getHeaders();
    HttpHeaders httpRequestHeaders = httpRequestMessage.headers();
    for(Map.Entry<String, String> entry: defaultHeaders.entrySet()) {
        HttpTransportClient.addHeader(httpRequestHeaders, entry.getKey(), entry.getValue());
    }
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.VERSION, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.USER_AGENT, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.PAGE_SIZE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.PRE_TRIGGER_INCLUDE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.PRE_TRIGGER_EXCLUDE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.POST_TRIGGER_INCLUDE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.POST_TRIGGER_EXCLUDE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.AUTHORIZATION, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.INDEXING_DIRECTIVE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.MIGRATE_COLLECTION_DIRECTIVE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.CONSISTENCY_LEVEL, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.SESSION_TOKEN, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.PREFER, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.RESOURCE_TOKEN_EXPIRY, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.ENABLE_SCAN_IN_QUERY, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.EMIT_VERBOSE_TRACES_IN_QUERY, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.CAN_CHARGE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.CAN_THROTTLE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.ENABLE_LOW_PRECISION_ORDER_BY, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.ENABLE_LOGGING, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.IS_READ_ONLY_SCRIPT, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.CONTENT_SERIALIZATION_FORMAT, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.CONTINUATION, request.getContinuation());
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.ACTIVITY_ID, activityId.toString());
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.PARTITION_KEY, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.PARTITION_KEY_RANGE_ID, request);
    String dateHeader = HttpUtils.getDateHeader(documentServiceRequestHeaders);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.X_DATE, dateHeader);
    HttpTransportClient.addHeader(httpRequestHeaders, "Match", this.getMatch(request, resourceOperation));
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.IF_MODIFIED_SINCE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.A_IM, request);
    if (!request.getIsNameBased()) {
        HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.RESOURCE_ID, request.getResourceId());
    }
    HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.ENTITY_ID, request.entityId);
    String fanoutRequestHeader = request.getHeaders().get(WFConstants.BackendHeaders.IS_FANOUT_REQUEST);
    HttpTransportClient.addHeader(httpRequestMessage.headers(), WFConstants.BackendHeaders.IS_FANOUT_REQUEST, fanoutRequestHeader);
    if (request.getResourceType() == ResourceType.DocumentCollection) {
        HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.COLLECTION_PARTITION_INDEX, documentServiceRequestHeaders.get(WFConstants.BackendHeaders.COLLECTION_PARTITION_INDEX));
        HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.COLLECTION_SERVICE_INDEX, documentServiceRequestHeaders.get(WFConstants.BackendHeaders.COLLECTION_SERVICE_INDEX));
    }
    if (documentServiceRequestHeaders.get(WFConstants.BackendHeaders.BIND_REPLICA_DIRECTIVE) != null) {
        HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.BIND_REPLICA_DIRECTIVE, documentServiceRequestHeaders.get(WFConstants.BackendHeaders.BIND_REPLICA_DIRECTIVE));
        HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.PRIMARY_MASTER_KEY, documentServiceRequestHeaders.get(WFConstants.BackendHeaders.PRIMARY_MASTER_KEY));
        HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.SECONDARY_MASTER_KEY, documentServiceRequestHeaders.get(WFConstants.BackendHeaders.SECONDARY_MASTER_KEY));
        HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.PRIMARY_READONLY_KEY, documentServiceRequestHeaders.get(WFConstants.BackendHeaders.PRIMARY_READONLY_KEY));
        HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.SECONDARY_READONLY_KEY, documentServiceRequestHeaders.get(WFConstants.BackendHeaders.SECONDARY_READONLY_KEY));
    }
    if (documentServiceRequestHeaders.get(HttpConstants.HttpHeaders.CAN_OFFER_REPLACE_COMPLETE) != null) {
        HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.CAN_OFFER_REPLACE_COMPLETE, documentServiceRequestHeaders.get(HttpConstants.HttpHeaders.CAN_OFFER_REPLACE_COMPLETE));
    }
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.IS_QUERY, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.QUERY, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.IS_UPSERT, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.SUPPORT_SPATIAL_LEGACY_COORDINATES, request);
    HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.PARTITION_COUNT, request);
    HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.COLLECTION_RID, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.FILTER_BY_SCHEMA_RESOURCE_ID, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.USE_POLYGONS_SMALLER_THAN_AHEMISPHERE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.GATEWAY_SIGNATURE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.POPULATE_QUOTA_INFO, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.POPULATE_QUERY_METRICS, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.FORCE_QUERY_SCAN, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.RESPONSE_CONTINUATION_TOKEN_LIMIT_IN_KB, request);
    HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.REMOTE_STORAGE_TYPE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.SHARE_THROUGHPUT, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.POPULATE_PARTITION_STATISTICS, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.POPULATE_COLLECTION_THROUGHPUT_INFO, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.REMAINING_TIME_IN_MS_ON_CLIENT_REQUEST, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.CLIENT_RETRY_ATTEMPT_COUNT, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.TARGET_LSN, request);
    HttpTransportClient.addHeader(httpRequestHeaders, HttpConstants.HttpHeaders.TARGET_GLOBAL_COMMITTED_LSN, request);
    HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.FEDERATION_ID_FOR_AUTH, request);
    HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.FANOUT_OPERATION_STATE, request);
    HttpTransportClient.addHeader(httpRequestHeaders, WFConstants.BackendHeaders.ALLOW_TENTATIVE_WRITES, request);
    HttpTransportClient.addHeader(httpRequestHeaders, CustomHeaders.HttpHeaders.EXCLUDE_SYSTEM_PROPERTIES, request);
    return httpRequestMessage;
}