# URL Shortener - Class Diagram

This diagram captures the core classes, their fields/methods, and relationships in the current implementation.

```mermaid
classDiagram
    direction LR

    class UrlMapping {
      +String shortCode
      +String longUrl
      +LocalDateTime createdAt
      +boolean custom
      +String userId
    }

    class UrlMappingRepository {
      +boolean existsByShortCode(shortCode)
      +Optional UrlMapping findByLongUrl(longUrl)
      +List UserUrlCount countUrlsPerUser()
    }

    class UrlShortenerService {
      +ShortenResponse shortenUrl(ShortenRequest, baseUrl, userUuid)
      +ResponseEntity redirect(shortCode)
      +List UserMetricsResponse userMetrics()
    }

    class UrlShortenerServiceImpl {
      -UrlMappingRepository repository
      -SnowflakeIdGenerator idGenerator
      -Base62Encoder encoder
      +ShortenResponse shortenUrl(...)
      +ResponseEntity redirect(...)
      +List userMetrics()
    }

    class UrlApiController {
      +POST /api/v1/shorten
      +GET  /api/v1/metrics/users
    }

    class UrlWebController {
      +GET  /
      +POST /shorten
    }

    class RedirectController {
      +GET /{shortCode}
    }

    class Base62Encoder {
      +String toBase62(long)
      +long fromBase62(String)
    }

    class SnowflakeIdGenerator {
      +long nextId()
    }

    class ShortenRequest {
      +String longUrl
      +String customAlias
    }

    class ShortenResponse {
      +String shortCode
      +String shortUrl
      +LocalDateTime createdAt
    }

    class UserMetricsResponse {
      +String userId
      +long count
    }

    class InvalidUrlException
    class CustomAliasAlreadyExistsException
    class UrlNotFoundException

    UrlShortenerServiceImpl ..|> UrlShortenerService
    UrlShortenerServiceImpl --> UrlMappingRepository : uses
    UrlShortenerServiceImpl --> SnowflakeIdGenerator : uses
    UrlShortenerServiceImpl --> Base62Encoder : uses

    UrlApiController --> UrlShortenerService : uses
    UrlWebController --> UrlShortenerService : uses
    RedirectController --> UrlShortenerService : uses

    UrlMappingRepository --> UrlMapping : manages

    UrlApiController --> ShortenRequest
    UrlShortenerServiceImpl --> ShortenResponse
    UrlShortenerServiceImpl --> UserMetricsResponse

    InvalidUrlException <.. UrlShortenerServiceImpl
    CustomAliasAlreadyExistsException <.. UrlShortenerServiceImpl
    UrlNotFoundException <.. UrlShortenerServiceImpl
```

## Notes
- UrlShortenerServiceImpl implements the core logic: validation, optional custom aliases, Snowflake ID generation, Base62 encoding, persistence, and 301 redirects.
- UrlApiController exposes JSON endpoints, UrlWebController serves the Thymeleaf-based form, and RedirectController handles the short-code redirect.
- UrlMappingRepository adds a projection-based metrics query to count URLs per user.
