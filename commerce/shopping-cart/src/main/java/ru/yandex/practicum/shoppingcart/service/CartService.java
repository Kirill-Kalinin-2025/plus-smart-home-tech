package ru.yandex.practicum.shoppingcart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.dto.shoppingcart.ChangeProductQuantityRequest;
import ru.yandex.practicum.interaction.dto.shoppingcart.ShoppingCartDto;
import ru.yandex.practicum.interaction.feign.WarehouseClient;
import ru.yandex.practicum.shoppingcart.entity.Cart;
import ru.yandex.practicum.shoppingcart.entity.CartProduct;
import ru.yandex.practicum.shoppingcart.repository.CartRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final WarehouseClient warehouseClient;

    @Transactional(readOnly = true)
    public ShoppingCartDto getShoppingCart(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не должно быть пустым");
        }

        Cart cart = getOrCreateCart(username);
        return toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Integer> products) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не должно быть пустым");
        }

        Cart cart = getOrCreateCart(username);

        if ("DEACTIVATE".equals(cart.getState())) {
            throw new IllegalStateException("Корзина деактивирована");
        }

        // Проверяем наличие на складе через Feign-клиент
        ShoppingCartDto checkCart = ShoppingCartDto.builder()
                .shoppingCartId(cart.getCartId())
                .products(products)
                .build();

        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(checkCart);
            log.info("Проверка склада пройдена для корзины {}", cart.getCartId());
        } catch (Exception e) {
            log.error("Ошибка проверки склада: {}", e.getMessage());
            throw new RuntimeException("Недостаточно товаров на складе: " + e.getMessage());
        }

        // Добавляем товары в корзину
        for (Map.Entry<UUID, Integer> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();

            Optional<CartProduct> existing = cart.getCartProducts().stream()
                    .filter(cp -> cp.getProductId().equals(productId))
                    .findFirst();

            if (existing.isPresent()) {
                existing.get().setQuantity(existing.get().getQuantity() + quantity);
            } else {
                CartProduct newProduct = CartProduct.builder()
                        .cart(cart)
                        .productId(productId)
                        .quantity(quantity)
                        .build();
                cart.getCartProducts().add(newProduct);
            }
        }

        cart = cartRepository.save(cart);
        log.info("Товары добавлены в корзину пользователя {}", username);
        return toDto(cart);
    }

    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не должно быть пустым");
        }

        Cart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Корзина не найдена для пользователя: " + username));

        cart.setState("DEACTIVATE");
        cartRepository.save(cart);
        log.info("Корзина пользователя {} деактивирована", username);
    }

    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не должно быть пустым");
        }

        final Cart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Корзина не найдена для пользователя: " + username));

        // Проверяем, что все товары есть в корзине
        boolean allExist = productIds.stream()
                .allMatch(pid -> cart.getCartProducts().stream()
                        .anyMatch(cp -> cp.getProductId().equals(pid)));

        if (!allExist) {
            throw new RuntimeException("Некоторые товары отсутствуют в корзине");
        }

        cart.getCartProducts().removeIf(cp -> productIds.contains(cp.getProductId()));
        Cart savedCart = cartRepository.save(cart);
        log.info("Товары удалены из корзины пользователя {}", username);
        return toDto(savedCart);
    }

    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не должно быть пустым");
        }

        final Cart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Корзина не найдена для пользователя: " + username));

        CartProduct product = cart.getCartProducts().stream()
                .filter(cp -> cp.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Товар не найден в корзине: " + request.getProductId()));

        product.setQuantity(request.getNewQuantity());
        Cart savedCart = cartRepository.save(cart);
        log.info("Изменено количество товара {} в корзине пользователя {}: {}",
                request.getProductId(), username, request.getNewQuantity());
        return toDto(savedCart);
    }

    private Cart getOrCreateCart(String username) {
        return cartRepository.findByUsername(username)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .cartId(UUID.randomUUID())
                            .username(username)
                            .state("ACTIVE")
                            .cartProducts(new ArrayList<>())
                            .build();
                    log.info("Создана новая корзина для пользователя {}", username);
                    return cartRepository.save(newCart);
                });
    }

    private ShoppingCartDto toDto(Cart cart) {
        Map<UUID, Integer> products = new HashMap<>();
        if (cart.getCartProducts() != null) {
            products = cart.getCartProducts().stream()
                    .collect(Collectors.toMap(
                            CartProduct::getProductId,
                            CartProduct::getQuantity
                    ));
        }

        return ShoppingCartDto.builder()
                .shoppingCartId(cart.getCartId())
                .products(products)
                .build();
    }
}