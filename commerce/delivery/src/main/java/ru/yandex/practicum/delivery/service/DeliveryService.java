package ru.yandex.practicum.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.dto.delivery.DeliveryDto;
import ru.yandex.practicum.interaction.dto.delivery.DeliveryState;
import ru.yandex.practicum.interaction.dto.order.OrderDto;
import ru.yandex.practicum.interaction.dto.warehouse.AddressDto;
import ru.yandex.practicum.interaction.dto.warehouse.ShippedToDeliveryRequest;
import ru.yandex.practicum.interaction.feign.OrderClient;
import ru.yandex.practicum.interaction.feign.WarehouseClient;
import ru.yandex.practicum.delivery.entity.Delivery;
import ru.yandex.practicum.delivery.repository.DeliveryRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final WarehouseClient warehouseClient;
    private final OrderClient orderClient;

    @Transactional
    public DeliveryDto planDelivery(DeliveryDto deliveryDto) {
        Delivery delivery = Delivery.builder()
                .deliveryId(deliveryDto.getDeliveryId() != null ? deliveryDto.getDeliveryId() : UUID.randomUUID())
                .orderId(deliveryDto.getOrderId())
                .fromCountry(deliveryDto.getFromAddress().getCountry())
                .fromCity(deliveryDto.getFromAddress().getCity())
                .fromStreet(deliveryDto.getFromAddress().getStreet())
                .fromHouse(deliveryDto.getFromAddress().getHouse())
                .fromFlat(deliveryDto.getFromAddress().getFlat())
                .toCountry(deliveryDto.getToAddress().getCountry())
                .toCity(deliveryDto.getToAddress().getCity())
                .toStreet(deliveryDto.getToAddress().getStreet())
                .toHouse(deliveryDto.getToAddress().getHouse())
                .toFlat(deliveryDto.getToAddress().getFlat())
                .state(DeliveryState.CREATED)
                .build();

        delivery = deliveryRepository.save(delivery);
        log.info("Создана доставка: {} для заказа {}", delivery.getDeliveryId(), delivery.getOrderId());
        return toDto(delivery);
    }

    @Transactional
    public void deliverySuccessful(UUID orderId) {
        final Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Доставка не найдена для заказа: " + orderId));

        delivery.setState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);

        // Уведомляем сервис заказов
        orderClient.delivery(orderId);
        log.info("Доставка {} успешно завершена", delivery.getDeliveryId());
    }

    @Transactional
    public void deliveryPicked(UUID orderId) {
        final Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Доставка не найдена для заказа: " + orderId));

        delivery.setState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);
        log.info("Товары для заказа {} получены в доставку", orderId);
    }

    @Transactional
    public void deliveryFailed(UUID orderId) {
        final Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Доставка не найдена для заказа: " + orderId));

        delivery.setState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);

        // Уведомляем сервис заказов
        orderClient.deliveryFailed(orderId);
        log.info("Доставка {} не удалась", delivery.getDeliveryId());
    }

    @Transactional(readOnly = true)
    public Double deliveryCost(OrderDto orderDto) {
        // Получаем адрес склада
        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();

        // Базовая стоимость
        double cost = 5.0;

        // Коэффициент адреса склада
        double warehouseFactor = warehouseAddress.getStreet().contains("ADDRESS_2") ? 2.0 : 1.0;
        cost = cost + (5.0 * warehouseFactor);

        // Хрупкость
        if (Boolean.TRUE.equals(orderDto.getFragile())) {
            cost = cost + (cost * 0.2);
        }

        // Вес
        if (orderDto.getDeliveryWeight() != null) {
            cost = cost + (orderDto.getDeliveryWeight() * 0.3);
        }

        // Объём
        if (orderDto.getDeliveryVolume() != null) {
            cost = cost + (orderDto.getDeliveryVolume() * 0.2);
        }

        // Адрес доставки (сравниваем улицы)
        String warehouseStreet = warehouseAddress.getStreet();
        // У заказа нет адреса доставки, поэтому просто добавляем 0.2
        cost = cost + (cost * 0.2);

        log.info("Расчёт стоимости доставки: {}", cost);
        return cost;
    }

    private DeliveryDto toDto(Delivery delivery) {
        return DeliveryDto.builder()
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .fromAddress(AddressDto.builder()
                        .country(delivery.getFromCountry())
                        .city(delivery.getFromCity())
                        .street(delivery.getFromStreet())
                        .house(delivery.getFromHouse())
                        .flat(delivery.getFromFlat())
                        .build())
                .toAddress(AddressDto.builder()
                        .country(delivery.getToCountry())
                        .city(delivery.getToCity())
                        .street(delivery.getToStreet())
                        .house(delivery.getToHouse())
                        .flat(delivery.getToFlat())
                        .build())
                .deliveryState(delivery.getState())
                .build();
    }
}