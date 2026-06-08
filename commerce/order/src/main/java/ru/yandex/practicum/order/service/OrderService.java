package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.dto.order.*;
import ru.yandex.practicum.interaction.dto.warehouse.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.interaction.dto.warehouse.BookedProductsDto;
import ru.yandex.practicum.interaction.feign.DeliveryClient;
import ru.yandex.practicum.interaction.feign.PaymentClient;
import ru.yandex.practicum.interaction.feign.WarehouseClient;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderProduct;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WarehouseClient warehouseClient;
    private final PaymentClient paymentClient;
    private final DeliveryClient deliveryClient;

    @Transactional(readOnly = true)
    public List<OrderDto> getClientOrders(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не должно быть пустым");
        }
        return orderRepository.findByUsername(username).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDto createNewOrder(CreateNewOrderRequest request) {
        // Проверяем наличие на складе
        warehouseClient.checkProductQuantityEnoughForShoppingCart(request.getShoppingCart());

        // Создаём заказ
        final Order order = Order.builder()
                .orderId(UUID.randomUUID())
                .username("user")
                .shoppingCartId(request.getShoppingCart().getShoppingCartId())
                .state(OrderState.NEW)
                .orderProducts(new ArrayList<>())
                .build();

        // Добавляем товары
        request.getShoppingCart().getProducts().forEach((productId, quantity) -> {
            OrderProduct orderProduct = OrderProduct.builder()
                    .order(order)
                    .productId(productId)
                    .quantity(quantity)
                    .build();
            order.getOrderProducts().add(orderProduct);
        });

        Order savedOrder = orderRepository.save(order);
        log.info("Создан новый заказ: {}", savedOrder.getOrderId());
        return toDto(savedOrder);
    }

    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        final Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + request.getOrderId()));

        // Возвращаем товары на склад
        warehouseClient.acceptReturn(request.getProducts());

        // Уменьшаем количество в заказе
        request.getProducts().forEach((productId, quantity) -> {
            order.getOrderProducts().stream()
                    .filter(op -> op.getProductId().equals(productId))
                    .findFirst()
                    .ifPresent(op -> {
                        int newQty = op.getQuantity() - quantity;
                        if (newQty <= 0) {
                            order.getOrderProducts().remove(op);
                        } else {
                            op.setQuantity(newQty);
                        }
                    });
        });

        order.setState(OrderState.PRODUCT_RETURNED);
        Order savedOrder = orderRepository.save(order);
        log.info("Возврат товаров для заказа {}", savedOrder.getOrderId());
        return toDto(savedOrder);
    }

    @Transactional
    public OrderDto payment(UUID orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + orderId));
        order.setState(OrderState.PAID);
        Order savedOrder = orderRepository.save(order);
        return toDto(savedOrder);
    }

    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + orderId));
        order.setState(OrderState.PAYMENT_FAILED);
        Order savedOrder = orderRepository.save(order);
        return toDto(savedOrder);
    }

    @Transactional
    public OrderDto delivery(UUID orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + orderId));
        order.setState(OrderState.DELIVERED);
        Order savedOrder = orderRepository.save(order);
        return toDto(savedOrder);
    }

    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + orderId));
        order.setState(OrderState.DELIVERY_FAILED);
        Order savedOrder = orderRepository.save(order);
        return toDto(savedOrder);
    }

    @Transactional
    public OrderDto complete(UUID orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + orderId));
        order.setState(OrderState.COMPLETED);
        Order savedOrder = orderRepository.save(order);
        return toDto(savedOrder);
    }

    @Transactional
    public OrderDto calculateTotalCost(UUID orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + orderId));

        OrderDto orderDto = toDto(order);
        Double totalCost = paymentClient.getTotalCost(orderDto);
        order.setTotalPrice(totalCost);
        Order savedOrder = orderRepository.save(order);
        return toDto(savedOrder);
    }

    @Transactional
    public OrderDto calculateDeliveryCost(UUID orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + orderId));

        OrderDto orderDto = toDto(order);
        Double deliveryCost = deliveryClient.deliveryCost(orderDto);
        order.setDeliveryPrice(deliveryCost);
        Order savedOrder = orderRepository.save(order);
        return toDto(savedOrder);
    }

    @Transactional
    public OrderDto assembly(UUID orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + orderId));

        Map<UUID, Integer> products = order.getOrderProducts().stream()
                .collect(Collectors.toMap(OrderProduct::getProductId, OrderProduct::getQuantity));

        AssemblyProductsForOrderRequest request = AssemblyProductsForOrderRequest.builder()
                .orderId(orderId)
                .products(products)
                .build();

        BookedProductsDto booked = warehouseClient.assemblyProductsForOrder(request);

        order.setDeliveryWeight(booked.getDeliveryWeight());
        order.setDeliveryVolume(booked.getDeliveryVolume());
        order.setFragile(booked.getFragile());
        order.setState(OrderState.ASSEMBLED);
        Order savedOrder = orderRepository.save(order);
        return toDto(savedOrder);
    }

    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + orderId));
        order.setState(OrderState.ASSEMBLY_FAILED);
        Order savedOrder = orderRepository.save(order);
        return toDto(savedOrder);
    }

    private OrderDto toDto(Order order) {
        Map<UUID, Integer> products = new HashMap<>();
        if (order.getOrderProducts() != null) {
            products = order.getOrderProducts().stream()
                    .collect(Collectors.toMap(OrderProduct::getProductId, OrderProduct::getQuantity));
        }

        return OrderDto.builder()
                .orderId(order.getOrderId())
                .shoppingCartId(order.getShoppingCartId())
                .products(products)
                .paymentId(order.getPaymentId())
                .deliveryId(order.getDeliveryId())
                .state(order.getState())
                .deliveryWeight(order.getDeliveryWeight())
                .deliveryVolume(order.getDeliveryVolume())
                .fragile(order.getFragile())
                .totalPrice(order.getTotalPrice())
                .deliveryPrice(order.getDeliveryPrice())
                .productPrice(order.getProductPrice())
                .build();
    }
}