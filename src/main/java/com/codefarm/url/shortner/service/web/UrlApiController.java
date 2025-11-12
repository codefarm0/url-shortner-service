package com.codefarm.url.shortner.service.web;

import com.codefarm.url.shortner.service.core.UrlShortenerService;
import com.codefarm.url.shortner.service.web.dto.ShortenRequest;
import com.codefarm.url.shortner.service.web.dto.ShortenResponse;
import com.codefarm.url.shortner.service.web.dto.UserMetricsResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UrlApiController {

    private final UrlShortenerService service;

    public UrlApiController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@RequestBody ShortenRequest request,
                                                   @RequestHeader(value = "user_uuid", required = false) String userUuid,
                                                   HttpServletRequest httpRequest) {
        String baseUrl = UrlApiController.getBaseUrl(httpRequest);
        ShortenResponse response = service.shortenUrl(request, baseUrl, userUuid);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/metrics/users")
    public ResponseEntity<java.util.List<UserMetricsResponse>> usersMetrics() {
        return ResponseEntity.ok(service.userMetrics());
    }

    private static String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean isDefault = (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443);
        return scheme + "://" + host + (isDefault ? "" : (":" + port));
    }
}


