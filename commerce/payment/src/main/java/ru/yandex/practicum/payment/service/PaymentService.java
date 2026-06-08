package ru.yandex.practicum.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.dto.order.OrderDto;
import ru.yandex.practicum.interaction.dto.payment.PaymentDto;
import ru.yandex.practicum.interaction.dto.payment.PaymentState;
import ru.yandex.practicum.interaction.feign.OrderClient;
import ru.yandex.practicum.interaction.feign.ShoppingStoreClient;
import ru.yandex.practicum.payment.entity.Payment;
import ru.yandex.practicum.payment.repository.PaymentRepository;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ShoppingStoreClient shoppingStoreClient;
    private final OrderClient orderClient;

    @Transactional
    public PaymentDto payment(OrderDto orderDto) {
        Double totalCost = getTotalCost(orderDto);
        Double deliveryCost = orderDto.getDeliveryPrice() != null ? orderDto.getDeliveryPrice() : 0.0;
        Double productCost = productCost(orderDto);

        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .orderId(orderDto.getOrderId())
                .totalPayment(totalCost)
                .deliveryTotal(deliveryCost)
                .feeTotal(totalCost - productCost - deliveryCost)
                .state(PaymentState.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Создан платёж: {} для заказа {}", payment.getPaymentId(), orderDto.getOrderId());

        return toDto(payment);
    }

    @Transactional(readOnly = true)
    public Double getTotalCost(OrderDto orderDto) {
        Double productCost = productCost(orderDto);
        Double deliveryCost = orderDto.getDeliveryPrice() != null ? orderDto.getDeliveryPrice() : 0.0;

        // НДС = 10% от стоимости товаров
        Double vat = productCost * 0.1;

        // Итого = товары + НДС + доставка
        return productCost + vat + deliveryCost;
    }

    @Transactional(readOnly = true)
    public Double productCost(OrderDto orderDto) {
        double total = 0.0;

        if (orderDto.getProducts() != null) {
            for (Map.Entry<UUID, Integer> entry : orderDto.getProducts().entrySet()) {
                UUID productId = entry.getKey();
                Integer quantity = entry.getValue();

                try {
                    var productDto = shoppingStoreClient.getProduct(productId);
                    total += productDto.getPrice() * quantity;
                } catch (Exception e) {
                    log.error("Ошибка получения товара {}: {}", productId, e.getMessage());
                }
            }
        }

        return total;
    }

    @Transactional
    public void paymentSuccess(UUID paymentId) {
        final Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Платёж не найден: " + paymentId));

        payment.setState(PaymentState.SUCCESS);
        paymentRepository.save(payment);

        // Уведомляем сервис заказов об успешной оплате
        orderClient.payment(payment.getOrderId());
        log.info("Платёж {} успешно обработан", paymentId);
    }

    @Transactional
    public void paymentFailed(UUID paymentId) {
        final Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Платёж не найден: " + paymentId));

        payment.setState(PaymentState.FAILED);
        paymentRepository.save(payment);

        // Уведомляем сервис заказов об ошибке оплаты
        orderClient.paymentFailed(payment.getOrderId());
        log.info("Платёж {} не удался", paymentId);
    }

    private PaymentDto toDto(Payment payment) {
        return PaymentDto.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .totalPayment(payment.getTotalPayment())
                .deliveryTotal(payment.getDeliveryTotal())
                .feeTotal(payment.getFeeTotal())
                .state(payment.getState())
                .build();
    }
}