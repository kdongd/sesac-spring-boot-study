package com.example.wishlistbudget.repository;

import com.example.wishlistbudget.domain.CollectionItem;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository {

    CollectionItem save(String name, int price);

    List<CollectionItem> findAll();

    Optional<CollectionItem> findById(Long id);
}