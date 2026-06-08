package ru.yandex.practicum.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.interaction.dto.order.OrderDto;
import ru.yandex.practicum.interaction.dto.payment.PaymentDto;
import ru.yandex.practicum.payment.service.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public PaymentDto payment(@RequestBody OrderDto orderDto) {
        return paymentService.payment(orderDto);
    }

    @PostMapping("/totalCost")
    public Double getTotalCost(@RequestBody OrderDto orderDto) {
        return paymentService.getTotalCost(orderDto);
    }

    @PostMapping("/refund")
    public void paymentSuccess(@RequestBody UUID paymentId) {
        paymentService.paymentSuccess(paymentId);
    }

    @PostMapping("/productCost")
    public Double productCost(@RequestBody OrderDto orderDto) {
        return paymentService.productCost(orderDto);
    }

    @PostMapping("/failed")
    public void paymentFailed(@RequestBody UUID paymentId) {
        paymentService.paymentFailed(paymentId);
    }
}