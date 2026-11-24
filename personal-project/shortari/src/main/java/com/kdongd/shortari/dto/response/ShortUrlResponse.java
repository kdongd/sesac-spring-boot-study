package com.kdongd.shortari.dto.response;

import com.kdongd.shortari.domain.ShortUrl;
import com.kdongd.shortari.domain.UrlMode;

import java.time.LocalDateTime;

public record ShortUrlResponse(
        Long id,
        String originalUrl,
        String shortCode,
        String shortUrl,
        String displayName,
        UrlMode mode,
        LocalDateTime createdAt
) {

    public static ShortUrlResponse from(
            ShortUrl shortUrl,
            String baseUrl
    ) {

        String generatedUrl = baseUrl
                + "/r/"
                + shortUrl.shortCode();

        return new ShortUrlResponse(
                shortUrl.id(),
                shortUrl.originalUrl(),
                shortUrl.shortCode(),
                generatedUrl,
                shortUrl.displayName(),
                shortUrl.urlMode(),
                shortUrl.createdAt()
        );
    }
}