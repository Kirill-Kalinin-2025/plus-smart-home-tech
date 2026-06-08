package ru.yandex.practicum.shoppingstore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.interaction.dto.shoppingstore.ProductCategory;
import ru.yandex.practicum.interaction.dto.shoppingstore.ProductState;
import ru.yandex.practicum.shoppingstore.entity.Product;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByProductStateAndProductCategory(ProductState state, ProductCategory category, Pageable pageable);

    Page<Product> findByProductState(ProductState state, Pageable pageable);
}