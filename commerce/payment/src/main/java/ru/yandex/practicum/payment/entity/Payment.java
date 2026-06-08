package ru.yandex.practicum.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.interaction.dto.payment.PaymentState;

import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "total_payment")
    private Double totalPayment;

    @Column(name = "delivery_total")
    private Double deliveryTotal;

    @Column(name = "fee_total")
    private Double feeTotal;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentState state;
}