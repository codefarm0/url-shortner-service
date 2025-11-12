package com.codefarm.url.shortner.service.web.dto;

import java.time.LocalDateTime;

public record ShortenResponse(String shortCode, String shortUrl, LocalDateTime createdAt) {}


