package ru.yandex.practicum.shoppingstore.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.interaction.dto.shoppingstore.ProductCategory;
import ru.yandex.practicum.interaction.dto.shoppingstore.ProductState;
import ru.yandex.practicum.interaction.dto.shoppingstore.QuantityState;

import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "image_src")
    private String imageSrc;

    @Column(name = "quantity_state", nullable = false)
    @Enumerated(EnumType.STRING)
    private QuantityState quantityState;

    @Column(name = "product_state", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductState productState;

    @Column(name = "product_category", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductCategory productCategory;

    @Column(name = "price", nullable = false)
    private Double price;
}