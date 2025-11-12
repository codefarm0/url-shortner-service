package com.codefarm.url.shortner.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_mappings")
public class UrlMapping {

    @Id
    @Column(name = "short_code", nullable = false, length = 16)
    private String shortCode;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_custom", nullable = false)
    private boolean custom;

    @Column(name = "user_id")
    private String userId;

    protected UrlMapping() {
        // JPA only
    }

    public UrlMapping(String shortCode, String longUrl, LocalDateTime createdAt, boolean custom) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.createdAt = createdAt;
        this.custom = custom;
    }

    public UrlMapping(String shortCode, String longUrl, LocalDateTime createdAt, boolean custom, String userId) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.createdAt = createdAt;
        this.custom = custom;
        this.userId = userId;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isCustom() {
        return custom;
    }

    public String getUserId() {
        return userId;
    }
}


