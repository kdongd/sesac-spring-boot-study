package com.kdongd.shortari.domain;

import java.time.LocalDateTime;

public record ShortUrl(
        Long id,
        String originalUrl,
        String shortCode,
        String displayName,
        UrlMode urlMode,
        LocalDateTime createdAt
)
{}
