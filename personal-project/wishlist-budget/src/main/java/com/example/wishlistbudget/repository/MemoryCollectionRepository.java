package com.example.wishlistbudget.repository;

import com.example.wishlistbudget.domain.CollectionItem;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class MemoryCollectionRepository implements CollectionRepository {

    private final List<CollectionItem> items = new ArrayList<>();

    private long sequence = 0L;

    // 아이템 저장
    @Override
    public CollectionItem save(String name, int price) {

        sequence++;

        CollectionItem item = new CollectionItem(
                sequence,
                name,
                price
        );

        items.add(item);

        return item;
    }

    // 전체 아이템 조회
    @Override
    public List<CollectionItem> findAll() {
        return new ArrayList<>(items);
    }

    // ID로 아이템 조회
    @Override
    public Optional<CollectionItem> findById(Long id) {

        for (CollectionItem item : items) {

            if (item.id().equals(id)) {
                return Optional.of(item);
            }
        }

        return Optional.empty();
    }
}