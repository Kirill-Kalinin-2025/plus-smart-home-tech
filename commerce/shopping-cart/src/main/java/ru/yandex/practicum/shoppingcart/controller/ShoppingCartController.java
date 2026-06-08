package ru.yandex.practicum.shoppingcart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.interaction.dto.shoppingcart.ChangeProductQuantityRequest;
import ru.yandex.practicum.interaction.dto.shoppingcart.ShoppingCartDto;
import ru.yandex.practicum.shoppingcart.service.CartService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-cart")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final CartService cartService;

    @GetMapping
    public ShoppingCartDto getShoppingCart(@RequestParam String username) {
        return cartService.getShoppingCart(username);
    }

    @PutMapping
    public ShoppingCartDto addProductToShoppingCart(@RequestParam String username,
                                                    @RequestBody Map<UUID, Integer> products) {
        return cartService.addProductToShoppingCart(username, products);
    }

    @DeleteMapping
    public void deactivateCurrentShoppingCart(@RequestParam String username) {
        cartService.deactivateCurrentShoppingCart(username);
    }

    @PostMapping("/remove")
    public ShoppingCartDto removeFromShoppingCart(@RequestParam String username,
                                                  @RequestBody List<UUID> productIds) {
        return cartService.removeFromShoppingCart(username, productIds);
    }

    @PostMapping("/change-quantity")
    public ShoppingCartDto changeProductQuantity(@RequestParam String username,
                                                 @RequestBody ChangeProductQuantityRequest request) {
        return cartService.changeProductQuantity(username, request);
    }
}