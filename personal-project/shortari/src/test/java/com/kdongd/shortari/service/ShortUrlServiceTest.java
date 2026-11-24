
package com.kdongd.shortari.service;

import com.kdongd.shortari.domain.ShortUrl;
import com.kdongd.shortari.domain.UrlMode;
import com.kdongd.shortari.dto.request.ShortUrlCreateRequest;
import com.kdongd.shortari.dto.request.ShortUrlNameUpdateRequest;
import com.kdongd.shortari.dto.response.ShortUrlResponse;
import com.kdongd.shortari.exception.DuplicateDisplayNameException;
import com.kdongd.shortari.exception.InvalidUrlException;
import com.kdongd.shortari.exception.ShortCodeGenerationException;
import com.kdongd.shortari.exception.ShortUrlNotFoundException;
import com.kdongd.shortari.repository.ShortUrlRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private ShortCodeGenerator generator;

    private ShortUrlService service;

    private ShortUrl sampleUrl;

    @BeforeEach
    void setUp() {
        service = new ShortUrlService(
                repository,
                generator,
                new UrlNormalizer(),
                "http://localhost:8080"
        );

        sampleUrl = new ShortUrl(
                1L,
                "https://www.naver.com",
                "aB3xK912",
                "네이버",
                UrlMode.NAMING,
                LocalDateTime.of(2026, 11, 16, 12, 0)
        );
    }

    @Test
    @DisplayName("원본 URL을 입력하면 단축 URL이 정상적으로 생성된다")
    void createShortUrlTest() {
        ShortUrlCreateRequest request = new ShortUrlCreateRequest(
                "www.naver.com",
                null,
                UrlMode.SHORT
        );

        when(generator.generate()).thenReturn("aB3xK912");

        when(repository.save(
                eq("https://www.naver.com"),
                eq("aB3xK912"),
                isNull(),
                eq(UrlMode.SHORT)
        )).thenReturn(
                new ShortUrl(
                        1L,
                        "https://www.naver.com",
                        "aB3xK912",
                        null,
                        UrlMode.SHORT,
                        sampleUrl.createdAt()
                )
        );

        ShortUrlResponse response = service.create(request);

        assertEquals(
                "http://localhost:8080/r/aB3xK912",
                response.shortUrl()
        );

        assertEquals("https://www.naver.com", response.originalUrl());
        assertEquals(UrlMode.SHORT, response.mode());
    }

    @Test
    @DisplayName("NAMING 방식으로 생성하면 표시 이름이 함께 저장된다")
    void createNamingUrlTest() {
        ShortUrlCreateRequest request = new ShortUrlCreateRequest(
                "www.naver.com",
                "네이버",
                UrlMode.NAMING
        );

        when(generator.generate()).thenReturn("aB3xK912");

        when(repository.save(
                "https://www.naver.com",
                "aB3xK912",
                "네이버",
                UrlMode.NAMING
        )).thenReturn(sampleUrl);

        ShortUrlResponse response = service.create(request);

        assertEquals("네이버", response.displayName());
        assertEquals(UrlMode.NAMING, response.mode());

        verify(repository).existsByDisplayName("네이버");
    }

    @Test
    @DisplayName("같은 원본 URL을 여러 번 등록해도 서로 다른 단축 URL을 생성한다")
    void duplicateOriginalUrlAllowedTest() {
        ShortUrlCreateRequest request = new ShortUrlCreateRequest(
                "www.naver.com",
                null,
                UrlMode.SHORT
        );

        when(generator.generate())
                .thenReturn("aB3xK912", "X7kP2mQ9");

        when(repository.save(
                eq("https://www.naver.com"),
                anyString(),
                isNull(),
                eq(UrlMode.SHORT)
        )).thenAnswer(invocation -> {
            String code = invocation.getArgument(1);

            return new ShortUrl(
                    code.equals("aB3xK912") ? 1L : 2L,
                    "https://www.naver.com",
                    code,
                    null,
                    UrlMode.SHORT,
                    sampleUrl.createdAt()
            );
        });

        ShortUrlResponse first = service.create(request);
        ShortUrlResponse second = service.create(request);

        assertNotEquals(first.shortCode(), second.shortCode());
        assertEquals(first.originalUrl(), second.originalUrl());

        verify(repository, times(2)).save(
                eq("https://www.naver.com"),
                anyString(),
                isNull(),
                eq(UrlMode.SHORT)
        );
    }

    @Test
    @DisplayName("이미 등록된 표시 이름으로 URL을 생성하면 예외가 발생한다")
    void duplicateDisplayNameTest() {
        ShortUrlCreateRequest request = new ShortUrlCreateRequest(
                "www.naver.com",
                "네이버",
                UrlMode.NAMING
        );

        when(repository.existsByDisplayName("네이버"))
                .thenReturn(true);

        assertThrows(
                DuplicateDisplayNameException.class,
                () -> service.create(request)
        );

        verify(repository, never()).save(
                anyString(), anyString(), any(), any()
        );
    }

    @Test
    @DisplayName("단축 코드가 중복되면 새로운 코드를 생성하여 재시도한다")
    void retryDuplicateShortCodeTest() {
        ShortUrlCreateRequest request = new ShortUrlCreateRequest(
                "www.naver.com",
                null,
                UrlMode.SHORT
        );

        when(generator.generate())
                .thenReturn("aB3xK912", "X7kP2mQ9");

        when(repository.save(
                eq("https://www.naver.com"),
                anyString(),
                isNull(),
                eq(UrlMode.SHORT)
        ))
                .thenThrow(new DuplicateKeyException("Duplicate short_code"))
                .thenReturn(new ShortUrl(
                        2L,
                        "https://www.naver.com",
                        "X7kP2mQ9",
                        null,
                        UrlMode.SHORT,
                        sampleUrl.createdAt()
                ));

        ShortUrlResponse response = service.create(request);

        assertEquals("X7kP2mQ9", response.shortCode());
        verify(generator, times(2)).generate();
        verify(repository, times(2)).save(
                anyString(), anyString(), isNull(), eq(UrlMode.SHORT)
        );
    }

    @Test
    @DisplayName("단축 코드 생성이 10번 모두 충돌하면 예외가 발생한다")
    void shortCodeRetryExceededTest() {
        ShortUrlCreateRequest request = new ShortUrlCreateRequest(
                "www.naver.com",
                null,
                UrlMode.SHORT
        );

        when(generator.generate()).thenReturn("aB3xK912");

        when(repository.save(
                anyString(), anyString(), isNull(), eq(UrlMode.SHORT)
        )).thenThrow(new DuplicateKeyException("Duplicate short_code"));

        assertThrows(
                ShortCodeGenerationException.class,
                () -> service.create(request)
        );

        verify(generator, times(10)).generate();
    }

    @Test
    @DisplayName("NAMING 방식에서 표시 이름이 없으면 예외가 발생한다")
    void namingWithoutDisplayNameTest() {
        ShortUrlCreateRequest request = new ShortUrlCreateRequest(
                "www.naver.com",
                null,
                UrlMode.NAMING
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request)
        );
    }

    @Test
    @DisplayName("SHORT 방식에서 표시 이름을 입력하면 예외가 발생한다")
    void shortModeWithDisplayNameTest() {
        ShortUrlCreateRequest request = new ShortUrlCreateRequest(
                "www.naver.com",
                "네이버",
                UrlMode.SHORT
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request)
        );
    }

    @Test
    @DisplayName("잘못된 원본 URL을 입력하면 예외가 발생한다")
    void invalidOriginalUrlTest() {
        ShortUrlCreateRequest request = new ShortUrlCreateRequest(
                "잘못된 주소",
                null,
                UrlMode.SHORT
        );

        assertThrows(
                InvalidUrlException.class,
                () -> service.create(request)
        );
    }

    @Test
    @DisplayName("전체 단축 URL 목록을 조회할 수 있다")
    void findAllTest() {
        when(repository.findAll()).thenReturn(List.of(sampleUrl));

        List<ShortUrlResponse> responses = service.findAll();

        assertEquals(1, responses.size());
        assertEquals("네이버", responses.get(0).displayName());
        assertEquals("aB3xK912", responses.get(0).shortCode());
    }

    @Test
    @DisplayName("등록된 링크가 없으면 빈 목록을 반환한다")
    void findAllEmptyTest() {
        when(repository.findAll()).thenReturn(List.of());

        List<ShortUrlResponse> responses = service.findAll();

        assertTrue(responses.isEmpty());
    }

    @Test
    @DisplayName("ID로 단축 URL을 조회할 수 있다")
    void findByIdTest() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(sampleUrl));

        ShortUrlResponse response = service.findById(1L);

        assertEquals(1L, response.id());
        assertEquals("네이버", response.displayName());
    }

    @Test
    @DisplayName("존재하지 않는 ID를 조회하면 예외가 발생한다")
    void findByIdNotFoundTest() {
        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ShortUrlNotFoundException.class,
                () -> service.findById(999L)
        );
    }

    @Test
    @DisplayName("단축 코드로 원본 URL을 조회할 수 있다")
    void findOriginalUrlTest() {
        when(repository.findByShortCode("aB3xK912"))
                .thenReturn(Optional.of(sampleUrl));

        String originalUrl = service.findOriginalUrl("aB3xK912");

        assertEquals("https://www.naver.com", originalUrl);
    }

    @Test
    @DisplayName("존재하지 않는 단축 코드로 조회하면 예외가 발생한다")
    void findOriginalUrlNotFoundTest() {
        when(repository.findByShortCode("NOTFOUND"))
                .thenReturn(Optional.empty());

        assertThrows(
                ShortUrlNotFoundException.class,
                () -> service.findOriginalUrl("NOTFOUND")
        );
    }

    @Test
    @DisplayName("표시 이름을 정상적으로 수정할 수 있다")
    void updateDisplayNameTest() {
        ShortUrlNameUpdateRequest request =
                new ShortUrlNameUpdateRequest("네이버 메인");

        ShortUrl updatedUrl = new ShortUrl(
                1L,
                sampleUrl.originalUrl(),
                sampleUrl.shortCode(),
                "네이버 메인",
                sampleUrl.urlMode(),
                sampleUrl.createdAt()
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(sampleUrl))
                .thenReturn(Optional.of(updatedUrl));

        when(repository.updateDisplayName(1L, "네이버 메인"))
                .thenReturn(true);

        ShortUrlResponse response = service.updateName(1L, request);

        assertEquals("네이버 메인", response.displayName());
        verify(repository).updateDisplayName(1L, "네이버 메인");
    }

    @Test
    @DisplayName("이미 사용 중인 표시 이름으로 수정하면 예외가 발생한다")
    void updateDuplicateDisplayNameTest() {
        ShortUrlNameUpdateRequest request =
                new ShortUrlNameUpdateRequest("구글");

        when(repository.findById(1L))
                .thenReturn(Optional.of(sampleUrl));

        when(repository.existsByDisplayName("구글"))
                .thenReturn(true);

        assertThrows(
                DuplicateDisplayNameException.class,
                () -> service.updateName(1L, request)
        );

        verify(repository, never()).updateDisplayName(
                anyLong(), anyString()
        );
    }

    @Test
    @DisplayName("기존 표시 이름과 동일하게 수정하면 DB 업데이트를 수행하지 않는다")
    void updateSameDisplayNameTest() {
        ShortUrlNameUpdateRequest request =
                new ShortUrlNameUpdateRequest("네이버");

        when(repository.findById(1L))
                .thenReturn(Optional.of(sampleUrl));

        ShortUrlResponse response = service.updateName(1L, request);

        assertEquals("네이버", response.displayName());

        verify(repository, never()).updateDisplayName(
                anyLong(), anyString()
        );
    }

    @Test
    @DisplayName("존재하지 않는 링크의 이름을 수정하면 예외가 발생한다")
    void updateNameNotFoundTest() {
        ShortUrlNameUpdateRequest request =
                new ShortUrlNameUpdateRequest("새 이름");

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ShortUrlNotFoundException.class,
                () -> service.updateName(999L, request)
        );
    }

    @Test
    @DisplayName("등록된 단축 URL을 삭제할 수 있다")
    void deleteShortUrlTest() {
        when(repository.deleteById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> service.delete(1L));

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 단축 URL을 삭제하면 예외가 발생한다")
    void deleteShortUrlNotFoundTest() {
        when(repository.deleteById(999L)).thenReturn(false);

        assertThrows(
                ShortUrlNotFoundException.class,
                () -> service.delete(999L)
        );
    }
}
