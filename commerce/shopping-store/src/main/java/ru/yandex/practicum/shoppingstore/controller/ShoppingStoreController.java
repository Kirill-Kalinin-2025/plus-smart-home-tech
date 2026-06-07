package ru.yandex.practicum.shoppingstore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.interaction.dto.shoppingstore.ProductDto;
import ru.yandex.practicum.interaction.dto.shoppingstore.QuantityState;
import ru.yandex.practicum.interaction.dto.shoppingstore.SetProductQuantityStateRequest;
import ru.yandex.practicum.shoppingstore.service.ProductService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
@RequiredArgsConstructor
public class ShoppingStoreController {

    private final ProductService productService;

    @GetMapping
    public Page<ProductDto> getProducts(@RequestParam String category,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(defaultValue = "") List<String> sort) {
        // Объединяем все параметры сортировки через запятую
        String sortStr = sort != null && !sort.isEmpty() ? String.join(",", sort) : "productName,ASC";
        return productService.getProducts(category, page, size, sortStr);
    }

    @PutMapping
    public ProductDto createNewProduct(@RequestBody ProductDto productDto) {
        return productService.createNewProduct(productDto);
    }

    @PostMapping
    public ProductDto updateProduct(@RequestBody ProductDto productDto) {
        return productService.updateProduct(productDto);
    }

    @PostMapping("/removeProductFromStore")
    public boolean removeProductFromStore(@RequestBody UUID productId) {
        return productService.removeProductFromStore(productId);
    }

    @PostMapping("/quantityState")
    public boolean setProductQuantityState(@RequestParam UUID productId,
                                           @RequestParam QuantityState quantityState) {
        SetProductQuantityStateRequest request = new SetProductQuantityStateRequest();
        request.setProductId(productId);
        request.setQuantityState(quantityState);
        return productService.setProductQuantityState(request);
    }

    @GetMapping("/{productId}")
    public ProductDto getProduct(@PathVariable UUID productId) {
        return productService.getProduct(productId);
    }
}