
package com.example.wishlistbudget.service;

import com.example.wishlistbudget.domain.CollectionItem;
import com.example.wishlistbudget.exception.CollectionItemNotFoundException;
import com.example.wishlistbudget.repository.CollectionRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private CollectionRepository repository;

    @InjectMocks
    private CollectionService service;

    @Test
    @DisplayName("아이템 등록 시 이름의 앞뒤 공백을 제거하고 저장")
    void addItem() {

        CollectionItem savedItem = new CollectionItem(1L, "글러브", 250000);

        when(repository.save("글러브", 250000))
                .thenReturn(savedItem);

        CollectionItem result = service.addItem("  글러브  ", 250000);

        assertEquals(1L, result.id());
        assertEquals("글러브", result.name());
        assertEquals(250000, result.price());

        verify(repository).save("글러브", 250000);
    }

    @Test
    @DisplayName("등록된 아이템 전체 조회")
    void getAllItems() {

        List<CollectionItem> items = List.of(
                new CollectionItem(1L, "글러브", 250000),
                new CollectionItem(2L, "핸드랩", 45000)
        );

        when(repository.findAll()).thenReturn(items);

        List<CollectionItem> result = service.getAllItems();

        assertEquals(2, result.size());
        assertEquals("글러브", result.get(0).name());
        assertEquals("핸드랩", result.get(1).name());

        verify(repository).findAll();
    }

    @Test
    @DisplayName("ID로 아이템 조회")
    void getItem() {

        CollectionItem item = new CollectionItem(1L, "글러브", 250000);

        when(repository.findById(1L))
                .thenReturn(Optional.of(item));

        CollectionItem result = service.getItem(1L);

        assertEquals(item, result);

        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("이름이 비어 있거나 가격이 음수이면 저장되지 않음")
    void invalidItem() {

        assertThrows(IllegalArgumentException.class,
                () -> service.addItem("   ", 10000));

        assertThrows(IllegalArgumentException.class,
                () -> service.addItem(null, 10000));

        assertThrows(IllegalArgumentException.class,
                () -> service.addItem("글러브", -1000));

        verify(repository, never()).save(anyString(), anyInt());
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 예외 발생")
    void itemNotFound() {

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(CollectionItemNotFoundException.class,
                () -> service.getItem(999L));

        verify(repository).findById(999L);
    }

    @Test
    @DisplayName("예산 이하 아이템을 가격 오름차순으로 최대 3개 추천")
    void recommendItems() {

        List<CollectionItem> items = new ArrayList<>(List.of(
                new CollectionItem(1L, "글러브", 250000),
                new CollectionItem(2L, "핸드랩", 45000),
                new CollectionItem(3L, "마우스피스", 15000),
                new CollectionItem(4L, "무료 줄넘기", 0),
                new CollectionItem(5L, "운동 양말", 15000)
        ));

        when(repository.findAll()).thenReturn(items);

        List<CollectionItem> result = service.recommend(50000);

        assertEquals(3, result.size());
        assertEquals("무료 줄넘기", result.get(0).name());
        assertEquals("마우스피스", result.get(1).name());
        assertEquals("운동 양말", result.get(2).name());

        // 추천 후 원본 목록의 등록 순서가 유지되는지 검증
        assertEquals("글러브", items.get(0).name());

        verify(repository).findAll();
    }

    @Test
    @DisplayName("예산이 0원이면 무료 아이템만 추천")
    void recommendFreeItem() {

        List<CollectionItem> items = List.of(
                new CollectionItem(1L, "글러브", 250000),
                new CollectionItem(2L, "무료 줄넘기", 0)
        );

        when(repository.findAll()).thenReturn(items);

        List<CollectionItem> result = service.recommend(0);

        assertEquals(1, result.size());
        assertEquals("무료 줄넘기", result.get(0).name());
    }

    @Test
    @DisplayName("예산이 음수이면 예외가 발생한다")
    void invalidBudget() {

        assertThrows(IllegalArgumentException.class,
                () -> service.recommend(-1));

        verify(repository, never()).findAll();
    }
}
