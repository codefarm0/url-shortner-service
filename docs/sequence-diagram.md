# URL Shortener - Sequence Diagrams

This document illustrates two primary flows: URL shortening and redirection.

## 1) Shorten URL (API)

```mermaid
sequenceDiagram
    participant C as Client
    participant Api as UrlApiController
    participant Svc as UrlShortenerServiceImpl
    participant Repo as UrlMappingRepository
    participant Id as SnowflakeIdGenerator
    participant Enc as Base62Encoder

    C->>Api: POST /api/v1/shorten { longUrl, customAlias? }\nHeader: user_uuid?
    Api->>Svc: shortenUrl(request, baseUrl, userUuid)
    alt customAlias provided
        Svc->>Repo: existsByShortCode(alias)?
        Repo-->>Svc: boolean
        alt available
            Svc->>Repo: save(UrlMapping{ alias, longUrl, userId })
            Repo-->>Svc: saved
            Svc-->>Api: ShortenResponse(shortUrl)
        else taken
            Svc-->>Api: 409 CONFLICT (CustomAliasAlreadyExistsException)
        end
    else no customAlias
        Svc->>Repo: findByLongUrl(longUrl)
        Repo-->>Svc: Optional<UrlMapping>
        alt exists
            Svc-->>Api: ShortenResponse(existing)
        else new mapping
            Svc->>Id: nextId()
            Id-->>Svc: long id
            Svc->>Enc: toBase62(id)
            Enc-->>Svc: shortCode
            Svc->>Repo: save(UrlMapping{ shortCode, longUrl, userId })
            Repo-->>Svc: saved
            Svc-->>Api: ShortenResponse(shortUrl)
        end
    end
    Api-->>C: 200 OK { shortUrl }
```

### Notes
- The optional `user_uuid` header is stored with each mapping when present.
- When `customAlias` is provided, the service validates format and uniqueness before persisting.

## 2) Redirect

```mermaid
sequenceDiagram
    participant C as Client
    participant Red as RedirectController
    participant Svc as UrlShortenerServiceImpl
    participant Repo as UrlMappingRepository

    C->>Red: GET /{shortCode}
    Red->>Svc: redirect(shortCode)
    Svc->>Repo: findById(shortCode)
    alt found
        Repo-->>Svc: UrlMapping
        Svc-->>Red: ResponseEntity 301\nLocation: longUrl\nCache-Control: private, max-age=90\nX-Robots-Tag: noindex
        Red-->>C: 301 Moved Permanently
    else not found
        Repo-->>Svc: empty
        Svc-->>Red: 404 NOT FOUND (UrlNotFoundException)
        Red-->>C: 404 { error }
    end
```

### Notes
- The redirect uses a permanent 301 with caching hints mirroring the production behavior described in the design document.
- No cache layer is included in this implementation; lookups go directly through JPA/H2 (or your configured DB).
