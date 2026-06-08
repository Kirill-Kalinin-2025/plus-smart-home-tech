package ru.yandex.practicum.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.interaction.dto.shoppingcart.ShoppingCartDto;
import ru.yandex.practicum.interaction.dto.warehouse.*;
import ru.yandex.practicum.interaction.feign.WarehouseClient;
import ru.yandex.practicum.warehouse.service.WarehouseService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController implements WarehouseClient {

    private final WarehouseService warehouseService;

    @Override
    @PutMapping
    public void newProductInWarehouse(@RequestBody NewProductInWarehouseRequest request) {
        warehouseService.newProductInWarehouse(request);
    }

    @Override
    @PostMapping("/check")
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(@RequestBody ShoppingCartDto cart) {
        return warehouseService.checkAvailability(cart);
    }

    @Override
    @PostMapping("/add")
    public void addProductToWarehouse(@RequestBody AddProductToWarehouseRequest request) {
        warehouseService.addProductToWarehouse(request);
    }

    @Override
    @GetMapping("/address")
    public AddressDto getWarehouseAddress() {
        return warehouseService.getAddress();
    }

    @Override
    @PostMapping("/assembly")
    public BookedProductsDto assemblyProductsForOrder(@RequestBody AssemblyProductsForOrderRequest request) {
        return warehouseService.assemblyProductsForOrder(request);
    }

    @Override
    @PostMapping("/shipped")
    public void shippedToDelivery(@RequestBody ShippedToDeliveryRequest request) {
        warehouseService.shippedToDelivery(request);
    }

    @Override
    @PostMapping("/return")
    public void acceptReturn(@RequestBody Map<UUID, Integer> products) {
        warehouseService.acceptReturn(products);
    }
}