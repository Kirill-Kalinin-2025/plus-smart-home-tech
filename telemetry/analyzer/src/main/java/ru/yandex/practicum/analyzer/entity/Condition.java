package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;

@Entity
@Table(name = "conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConditionTypeAvro type;

    @Column(name = "operation", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConditionOperationAvro operation;

    @Column(name = "value")
    private Integer value;
}