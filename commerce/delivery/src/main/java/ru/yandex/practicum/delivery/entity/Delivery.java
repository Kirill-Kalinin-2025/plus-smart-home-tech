package ru.yandex.practicum.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.interaction.dto.delivery.DeliveryState;

import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "from_country")
    private String fromCountry;
    @Column(name = "from_city")
    private String fromCity;
    @Column(name = "from_street")
    private String fromStreet;
    @Column(name = "from_house")
    private String fromHouse;
    @Column(name = "from_flat")
    private String fromFlat;

    @Column(name = "to_country")
    private String toCountry;
    @Column(name = "to_city")
    private String toCity;
    @Column(name = "to_street")
    private String toStreet;
    @Column(name = "to_house")
    private String toHouse;
    @Column(name = "to_flat")
    private String toFlat;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliveryState state;
}