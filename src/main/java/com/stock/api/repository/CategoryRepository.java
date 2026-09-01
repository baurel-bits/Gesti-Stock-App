package com.stock.api.repository;

import com.stock.api.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByDeletedFalse();

    boolean existsByNameAndDeletedFalse(String name);
}
