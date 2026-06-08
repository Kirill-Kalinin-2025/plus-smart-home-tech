package ru.yandex.practicum.order.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.interaction.dto.order.OrderState;

import java.util.*;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "shopping_cart_id")
    private UUID shoppingCartId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderState state;

    @Column(name = "delivery_weight")
    private Double deliveryWeight;

    @Column(name = "delivery_volume")
    private Double deliveryVolume;

    @Column(name = "fragile")
    private Boolean fragile;

    @Column(name = "total_price")
    private Double totalPrice;

    @Column(name = "delivery_price")
    private Double deliveryPrice;

    @Column(name = "product_price")
    private Double productPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderProduct> orderProducts = new ArrayList<>();
}