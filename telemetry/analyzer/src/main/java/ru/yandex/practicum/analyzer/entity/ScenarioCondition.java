package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioCondition {

    @EmbeddedId
    private ScenarioConditionId id;

    @MapsId("scenarioId")
    @JoinColumn(name = "scenario_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Scenario scenario;

    @MapsId("sensorId")
    @JoinColumn(name = "sensor_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Sensor sensor;

    @MapsId("conditionId")
    @JoinColumn(name = "condition_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Condition condition;
}