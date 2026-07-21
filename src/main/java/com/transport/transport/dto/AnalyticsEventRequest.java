package com.transport.transport.dto;

import java.util.Map;

public record AnalyticsEventRequest(
    String visitorId,
    String sessionId,
    String installationId,
    String eventName,
    String platform,
    String page,
    Map<String, Object> metadata,
    String deviceType,
    String appVersion) {
}
