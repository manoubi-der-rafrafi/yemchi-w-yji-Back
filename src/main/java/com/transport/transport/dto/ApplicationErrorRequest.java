package com.transport.transport.dto;

import java.util.Map;

public record ApplicationErrorRequest(
    String message,
    String type,
    String source,
    String severity,
    String page,
    String endpoint,
    Integer httpStatus,
    String stackTrace,
    Map<String, Object> metadata,
    String appVersion,
    String deviceType) {
}
