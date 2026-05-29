package com.datashare;

public record FileResponse(
    String id,
    String fileName,
    Long fileSize,
    String expiresAt,
    String status,
    String downloadToken
) {}
