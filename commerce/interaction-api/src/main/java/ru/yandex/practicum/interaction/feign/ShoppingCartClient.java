package ru.yandex.practicum.interaction.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.interaction.dto.shoppingcart.ChangeProductQuantityRequest;
import ru.yandex.practicum.interaction.dto.shoppingcart.ShoppingCartDto;

@FeignClient(name = "shopping-cart")
public interface ShoppingCartClient {

    @GetMapping("/api/v1/shopping-cart/{username}")
    ShoppingCartDto getCart(@PathVariable String username);

    @PutMapping("/api/v1/shopping-cart/{username}")
    ShoppingCartDto addProduct(@PathVariable String username,
                               @RequestBody ChangeProductQuantityRequest request);

    @PostMapping("/api/v1/shopping-cart/{username}")
    ShoppingCartDto changeQuantity(@PathVariable String username,
                                   @RequestBody ChangeProductQuantityRequest request);

    @PostMapping("/api/v1/shopping-cart/{username}/remove")
    ShoppingCartDto removeProduct(@PathVariable String username,
                                  @RequestBody ChangeProductQuantityRequest request);

    @PostMapping("/api/v1/shopping-cart/{username}/deactivate")
    ShoppingCartDto deactivateCart(@PathVariable String username);
}