package com.codefarm.url.shortner.service.core;

import com.codefarm.url.shortner.service.exception.CustomAliasAlreadyExistsException;
import com.codefarm.url.shortner.service.exception.InvalidUrlException;
import com.codefarm.url.shortner.service.exception.UrlNotFoundException;
import com.codefarm.url.shortner.service.model.UrlMapping;
import com.codefarm.url.shortner.service.repository.UrlMappingRepository;
import com.codefarm.url.shortner.service.util.Base62Encoder;
import com.codefarm.url.shortner.service.util.SnowflakeIdGenerator;
import com.codefarm.url.shortner.service.web.dto.ShortenRequest;
import com.codefarm.url.shortner.service.web.dto.ShortenResponse;
import com.codefarm.url.shortner.service.web.dto.UserMetricsResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private final UrlMappingRepository repository;
    private final SnowflakeIdGenerator idGenerator;
    private final Base62Encoder encoder;

    public UrlShortenerServiceImpl(
            UrlMappingRepository repository,
            SnowflakeIdGenerator idGenerator,
            Base62Encoder encoder) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request, String requestBaseUrl, String userUuid) {
        String longUrl = normalizeUrl(request.longUrl());

        if (isOwnShortUrl(longUrl, requestBaseUrl)) {
            throw new InvalidUrlException("Cannot shorten a URL from this service. Provide the original long URL.");
        }

        Optional<UrlMapping> existing = repository.findByLongUrl(longUrl);
        if (existing.isPresent()) {
            String shortCode = existing.get().getShortCode();
            return new ShortenResponse(shortCode, buildShortUrl(requestBaseUrl, shortCode), existing.get().getCreatedAt());
        }

        String normalizedUserId = (userUuid == null || userUuid.isBlank()) ? null : userUuid.trim();

        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            String alias = request.customAlias().trim();
            validateAlias(alias);
            if (repository.existsByShortCode(alias)) {
                throw new CustomAliasAlreadyExistsException("Alias already in use");
            }
            UrlMapping mapping = new UrlMapping(alias, longUrl, LocalDateTime.now(), true, normalizedUserId);
            repository.save(mapping);
            return new ShortenResponse(alias, buildShortUrl(requestBaseUrl, alias), mapping.getCreatedAt());
        }

        String shortCode = generateUniqueShortCode();
        UrlMapping mapping = new UrlMapping(shortCode, longUrl, LocalDateTime.now(), false, normalizedUserId);
        repository.save(mapping);
        return new ShortenResponse(shortCode, buildShortUrl(requestBaseUrl, shortCode), mapping.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Void> redirect(String shortCode) {
        UrlMapping mapping = repository.findById(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short code not found: " + shortCode));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", mapping.getLongUrl());
        headers.add("Cache-Control", "private, max-age=90");
        headers.add("X-Robots-Tag", "noindex");
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<UserMetricsResponse> userMetrics() {
        return repository.countUrlsPerUser().stream()
                .map(r -> new UserMetricsResponse(r.getUserId(), r.getCount()))
                .toList();
    }

    private String generateUniqueShortCode() {
        for (int i = 0; i < 3; i++) {
            long id = idGenerator.nextId();
            String code = encoder.toBase62(id);
            if (!repository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate unique short code");
    }

    private static void validateAlias(String alias) {
        if (!alias.matches("^[a-zA-Z0-9_-]{1,32}$")) {
            throw new InvalidUrlException("Alias contains invalid characters");
        }
    }

    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL cannot be empty");
        }
        String trimmed = url.trim();
        String candidate = (trimmed.startsWith("http://") || trimmed.startsWith("https://")) ? trimmed : ("https://" + trimmed);
        try {
            URI uri = new URI(candidate);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new InvalidUrlException("Invalid URL format");
            }
            String scheme = uri.getScheme().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new InvalidUrlException("Only HTTP/HTTPS URLs are allowed");
            }
            return candidate;
        } catch (URISyntaxException _) {
            throw new InvalidUrlException("Invalid URL format");
        }
    }

    private static boolean isOwnShortUrl(String url, String baseUrl) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return false;

            URI base = new URI(baseUrl);
            String baseHost = base.getHost();
            if (baseHost == null) return false;

            String normalizedHost = host.toLowerCase().replace("www.", "");
            String normalizedBase = baseHost.toLowerCase().replace("www.", "");
            return normalizedHost.equals(normalizedBase) || normalizedHost.endsWith("." + normalizedBase);
        } catch (URISyntaxException _) {
            return false;
        }
    }

    private static String buildShortUrl(String baseUrl, String shortCode) {
        String normalized = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return normalized + shortCode;
    }
}


