package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioAction {

    @EmbeddedId
    private ScenarioActionId id;

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

    @MapsId("actionId")
    @JoinColumn(name = "action_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Action action;
}