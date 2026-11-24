package com.kdongd.shortari.service;

import com.kdongd.shortari.domain.ShortUrl;
import com.kdongd.shortari.domain.UrlMode;
import com.kdongd.shortari.dto.request.ShortUrlCreateRequest;
import com.kdongd.shortari.dto.request.ShortUrlNameUpdateRequest;
import com.kdongd.shortari.dto.response.ShortUrlResponse;
import com.kdongd.shortari.exception.DuplicateDisplayNameException;
import com.kdongd.shortari.exception.ShortCodeGenerationException;
import com.kdongd.shortari.exception.ShortUrlNotFoundException;
import com.kdongd.shortari.repository.ShortUrlRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShortUrlService {

    private static final int MAX_RETRY = 10;

    private final ShortUrlRepository repository;
    private final ShortCodeGenerator generator;
    private final UrlNormalizer normalizer;
    private final String baseUrl;

    public ShortUrlService(
            ShortUrlRepository repository,
            ShortCodeGenerator generator,
            UrlNormalizer normalizer,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.repository = repository;
        this.generator = generator;
        this.normalizer = normalizer;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    // 단축 URL 생성
    public ShortUrlResponse create(ShortUrlCreateRequest request) {

        String originalUrl = normalizer.normalize(request.originalUrl());

        UrlMode mode = request.mode();

        String displayName = null;

        if (mode == UrlMode.NAMING) {

            if (request.displayName() == null
                    || request.displayName().isBlank()) {
                throw new IllegalArgumentException("표시 이름은 필수입니다.");
            }

            displayName = request.displayName().trim();

            if (repository.existsByDisplayName(displayName)) {
                throw new DuplicateDisplayNameException();
            }

        } else if (request.displayName() != null
                && !request.displayName().isBlank()) {

            throw new IllegalArgumentException(
                    "SHORT 방식에서는 표시 이름을 입력할 수 없습니다."
            );
        }

        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {

            String shortCode = generator.generate();

            try {

                ShortUrl saved = repository.save(
                        originalUrl,
                        shortCode,
                        displayName,
                        mode
                );

                return ShortUrlResponse.from(saved, baseUrl);

            } catch (DuplicateKeyException e) {

                // 표시 이름이 중복된 경우에는 코드 재생성이 의미 없음
                if (displayName != null
                        && repository.existsByDisplayName(displayName)) {
                    throw new DuplicateDisplayNameException();
                }

                // 단축 코드 충돌이면 새로운 코드 생성 후 재시도
            }
        }

        throw new ShortCodeGenerationException();
    }

    // 전체 링크 조회
    public List<ShortUrlResponse> findAll() {

        return repository.findAll()
                .stream()
                .map(shortUrl -> ShortUrlResponse.from(shortUrl, baseUrl))
                .toList();
    }

    // ID로 단건 조회
    public ShortUrlResponse findById(Long id) {

        ShortUrl shortUrl = repository.findById(id)
                .orElseThrow(ShortUrlNotFoundException::new);

        return ShortUrlResponse.from(shortUrl, baseUrl);
    }

    // 단축 코드로 원본 URL 조회
    public String findOriginalUrl(String shortCode) {

        return repository.findByShortCode(shortCode)
                .orElseThrow(ShortUrlNotFoundException::new)
                .originalUrl();
    }

    // 표시 이름 수정
    public ShortUrlResponse updateName(
            Long id,
            ShortUrlNameUpdateRequest request
    ) {

        ShortUrl existing = repository.findById(id)
                .orElseThrow(ShortUrlNotFoundException::new);

        String displayName = request.displayName().trim();

        if (existing.displayName() != null
                && existing.displayName().equals(displayName)) {
            return ShortUrlResponse.from(existing, baseUrl);
        }

        if (repository.existsByDisplayName(displayName)) {
            throw new DuplicateDisplayNameException();
        }

        try {

            boolean updated = repository.updateDisplayName(id, displayName);

            if (!updated) {
                throw new ShortUrlNotFoundException();
            }

        } catch (DuplicateKeyException e) {
            throw new DuplicateDisplayNameException();
        }

        return findById(id);
    }

    // 링크 삭제
    public void delete(Long id) {

        boolean deleted = repository.deleteById(id);

        if (!deleted) {
            throw new ShortUrlNotFoundException();
        }
    }
}