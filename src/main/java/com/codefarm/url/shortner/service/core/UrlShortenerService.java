package com.codefarm.url.shortner.service.core;

import com.codefarm.url.shortner.service.web.dto.ShortenRequest;
import com.codefarm.url.shortner.service.web.dto.ShortenResponse;
import com.codefarm.url.shortner.service.web.dto.UserMetricsResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface UrlShortenerService {
    ShortenResponse shortenUrl(ShortenRequest request, String requestBaseUrl, String userUuid);
    ResponseEntity<Void> redirect(String shortCode);
    List<UserMetricsResponse> userMetrics();
}


