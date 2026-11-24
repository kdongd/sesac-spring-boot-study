package com.kdongd.shortari.repository;

import com.kdongd.shortari.domain.ShortUrl;
import com.kdongd.shortari.domain.UrlMode;

import java.util.List;
import java.util.Optional;

public interface ShortUrlRepository {

    // 단축 URL 저장
    ShortUrl save(
            String originalUrl,
            String shortCode,
            String displayName,
            UrlMode mode
    );

    // ID로 단건 조회
    Optional<ShortUrl> findById(Long id);

    // 단축 코드로 조회
    Optional<ShortUrl> findByShortCode(String shortCode);

    // 전체 링크 조회
    List<ShortUrl> findAll();

    // 표시 이름 중복 확인
    boolean existsByDisplayName(String displayName);

    // 표시 이름 수정
    boolean updateDisplayName(Long id, String displayName);

    // ID로 삭제
    boolean deleteById(Long id);
}