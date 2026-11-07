package com.example.wishlistbudget.service;

import com.example.wishlistbudget.domain.CollectionItem;
import com.example.wishlistbudget.exception.CollectionItemNotFoundException;
import com.example.wishlistbudget.repository.CollectionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CollectionService {

    private final CollectionRepository repository;

    public CollectionService(CollectionRepository repository) {
        this.repository = repository;
    }

    // 아이템 등록
    public CollectionItem addItem(String name, int price) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("아이템 이름은 필수입니다.");
        }

        if (price < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
        }

        String trimmedName = name.trim();

        return repository.save(trimmedName, price);
    }

    // 전체 아이템 조회
    public List<CollectionItem> getAllItems() {
        return repository.findAll();
    }

    // ID로 아이템 조회
    public CollectionItem getItem(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new CollectionItemNotFoundException(id));
    }

    // 예산 이하의 아이템 최대 3개 추천
    public List<CollectionItem> recommend(int budget) {

        if (budget < 0) {
            throw new IllegalArgumentException("예산은 0 이상이어야 합니다.");
        }

        List<CollectionItem> items = repository.findAll();

        List<CollectionItem> availableItems = new ArrayList<>();

        // 예산 이하 아이템만 선택
        for (CollectionItem item : items) {

            if (item.price() <= budget) {
                availableItems.add(item);
            }
        }

        // 가격 오름차순 정렬
        availableItems.sort(
                Comparator.comparingInt(CollectionItem::price)
        );

        // 최대 3개 추천
        List<CollectionItem> recommendations = new ArrayList<>();

        for (int i = 0; i < availableItems.size() && i < 3; i++) {
            recommendations.add(availableItems.get(i));
        }

        return recommendations;
    }
}