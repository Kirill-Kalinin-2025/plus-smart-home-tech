package ru.yandex.practicum.shoppingstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.dto.shoppingstore.*;
import ru.yandex.practicum.shoppingstore.entity.Product;
import ru.yandex.practicum.shoppingstore.repository.ProductRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(String category, int page, int size, String sort) {
        Sort sorting = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sorting);

        Page<Product> products;
        if (category != null && !category.isEmpty()) {
            ProductCategory productCategory = ProductCategory.valueOf(category.toUpperCase());
            products = productRepository.findByProductStateAndProductCategory("ACTIVE", productCategory, pageable);
        } else {
            products = productRepository.findByProductState("ACTIVE", pageable);
        }

        return products.map(this::toDto);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "productName");
        }

        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = Sort.Direction.ASC;

        if (parts.length > 1) {
            String dir = parts[1].trim().toUpperCase();
            if ("DESC".equals(dir)) {
                direction = Sort.Direction.DESC;
            }
        }

        // Маппинг имени свойства из DTO на поле entity
        String entityField = switch (property) {
            case "productName" -> "productName";
            case "price" -> "price";
            case "productCategory" -> "productCategory";
            case "quantityState" -> "quantityState";
            default -> "productName";
        };

        return Sort.by(direction, entityField);
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден: " + productId));
        return toDto(product);
    }

    @Transactional
    public ProductDto createNewProduct(ProductDto productDto) {
        Product product = Product.builder()
                .productId(UUID.randomUUID())
                .productName(productDto.getProductName())
                .description(productDto.getDescription())
                .imageSrc(productDto.getImageSrc())
                .price(productDto.getPrice())
                .productCategory(productDto.getProductCategory())
                .quantityState(productDto.getQuantityState() != null ? productDto.getQuantityState() : QuantityState.MANY)
                .productState(ProductState.ACTIVE)
                .build();

        product = productRepository.save(product);
        log.info("Добавлен новый товар: {}", product.getProductId());
        return toDto(product);
    }

    @Transactional
    public ProductDto updateProduct(ProductDto productDto) {
        Product product = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Товар не найден: " + productDto.getProductId()));

        if (productDto.getProductName() != null) product.setProductName(productDto.getProductName());
        if (productDto.getDescription() != null) product.setDescription(productDto.getDescription());
        if (productDto.getImageSrc() != null) product.setImageSrc(productDto.getImageSrc());
        if (productDto.getPrice() != null) product.setPrice(productDto.getPrice());
        if (productDto.getProductCategory() != null) product.setProductCategory(productDto.getProductCategory());
        if (productDto.getQuantityState() != null) product.setQuantityState(productDto.getQuantityState());

        product = productRepository.save(product);
        log.info("Обновлён товар: {}", product.getProductId());
        return toDto(product);
    }

    @Transactional
    public boolean removeProductFromStore(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден: " + productId));

        product.setProductState(ProductState.DEACTIVATE);
        productRepository.save(product);
        log.info("Товар деактивирован: {}", productId);
        return true;
    }

    @Transactional
    public boolean setProductQuantityState(SetProductQuantityStateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Товар не найден: " + request.getProductId()));

        product.setQuantityState(request.getQuantityState());
        productRepository.save(product);
        log.info("Изменено количество товара {}: {}", request.getProductId(), request.getQuantityState());
        return true;
    }

    private ProductDto toDto(Product product) {
        return ProductDto.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .imageSrc(product.getImageSrc())
                .quantityState(product.getQuantityState())
                .productState(product.getProductState())
                .productCategory(product.getProductCategory())
                .price(product.getPrice())
                .build();
    }
}