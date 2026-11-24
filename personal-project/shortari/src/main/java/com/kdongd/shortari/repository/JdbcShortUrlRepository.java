package com.kdongd.shortari.repository;

import com.kdongd.shortari.domain.ShortUrl;
import com.kdongd.shortari.domain.UrlMode;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcShortUrlRepository implements ShortUrlRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcShortUrlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ShortUrl> rowMapper = (rs, rowNum) ->
            new ShortUrl(
                    rs.getLong("id"),
                    rs.getString("original_url"),
                    rs.getString("short_code"),
                    rs.getString("display_name"),
                    UrlMode.valueOf(rs.getString("url_mode")),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );

    // 단축 URL 저장 후 생성된 ID로 조회
    @Override
    public ShortUrl save(
            String originalUrl,
            String shortCode,
            String displayName,
            UrlMode mode
    ) {

        String sql = """
                INSERT INTO short_url
                (original_url, short_code, display_name, url_mode)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, originalUrl);
            ps.setString(2, shortCode);
            ps.setString(3, displayName);
            ps.setString(4, mode.name());

            return ps;

        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException("생성된 ID를 확인할 수 없습니다.");
        }

        return findById(key.longValue())
                .orElseThrow(() ->
                        new IllegalStateException("저장된 URL을 조회할 수 없습니다.")
                );
    }

    // ID로 단건 조회
    @Override
    public Optional<ShortUrl> findById(Long id) {

        String sql = """
                SELECT *
                FROM short_url
                WHERE id = ?
                """;

        try {
            ShortUrl result = jdbcTemplate.queryForObject(
                    sql,
                    rowMapper,
                    id
            );

            return Optional.ofNullable(result);

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // 단축 코드로 원본 URL 조회
    @Override
    public Optional<ShortUrl> findByShortCode(String shortCode) {

        String sql = """
                SELECT *
                FROM short_url
                WHERE short_code = ?
                """;

        try {
            ShortUrl result = jdbcTemplate.queryForObject(
                    sql,
                    rowMapper,
                    shortCode
            );

            return Optional.ofNullable(result);

        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // 전체 링크 조회
    @Override
    public List<ShortUrl> findAll() {

        String sql = """
                SELECT *
                FROM short_url
                ORDER BY created_at DESC, id DESC
                """;

        return jdbcTemplate.query(sql, rowMapper);
    }

    // 표시 이름 중복 확인
    @Override
    public boolean existsByDisplayName(String displayName) {

        String sql = """
                SELECT COUNT(*)
                FROM short_url
                WHERE display_name = ?
                """;

        Long count = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                displayName
        );

        return count != null && count > 0;
    }

    // 표시 이름 수정
    @Override
    public boolean updateDisplayName(Long id, String displayName) {

        String sql = """
                UPDATE short_url
                SET display_name = ?
                WHERE id = ?
                """;

        int updatedRows = jdbcTemplate.update(
                sql,
                displayName,
                id
        );

        return updatedRows > 0;
    }

    // ID로 링크 삭제
    @Override
    public boolean deleteById(Long id) {

        String sql = """
                DELETE FROM short_url
                WHERE id = ?
                """;

        int deletedRows = jdbcTemplate.update(sql, id);

        return deletedRows > 0;
    }
}