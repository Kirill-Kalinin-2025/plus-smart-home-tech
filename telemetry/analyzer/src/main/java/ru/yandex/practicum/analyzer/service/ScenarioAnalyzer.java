package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.entity.Action;
import ru.yandex.practicum.analyzer.entity.Condition;
import ru.yandex.practicum.analyzer.entity.Scenario;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioAnalyzer {

    private final ScenarioRepository scenarioRepository;

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public void analyzeSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        log.info("=== АНАЛИЗ СНАПШОТА ===");
        log.info("Хаб: {}", hubId);
        log.info("Количество датчиков: {}", sensorsState.size());

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.info("Найдено сценариев для хаба {}: {}", hubId, scenarios.size());

        if (scenarios.isEmpty()) {
            log.warn("Нет сценариев для хаба {}", hubId);
            return;
        }

        for (Scenario scenario : scenarios) {
            log.info("Проверяю сценарий: {}", scenario.getName());
            if (checkScenarioConditions(scenario, sensorsState)) {
                log.info("✓ Сценарий '{}' АКТИВИРОВАН для хаба {}", scenario.getName(), hubId);
                executeScenarioActions(scenario, hubId, sensorsState);
            } else {
                log.info("✗ Сценарий '{}' не активирован для хаба {}", scenario.getName(), hubId);
            }
        }
    }

    private boolean checkScenarioConditions(Scenario scenario,
                                            Map<String, SensorStateAvro> sensorsState) {
        if (scenario.getConditions() == null || scenario.getConditions().isEmpty()) {
            return false;
        }

        return scenario.getConditions().stream()
                .allMatch(condition -> checkCondition(condition, sensorsState));
    }

    private boolean checkCondition(Condition condition,
                                   Map<String, SensorStateAvro> sensorsState) {
        String type = condition.getType();
        String operation = condition.getOperation();
        Integer requiredValue = condition.getValue();

        return sensorsState.values().stream()
                .filter(state -> getSensorValueByType(state, type) != null)
                .anyMatch(state -> {
                    Integer sensorValue = getSensorValueByType(state, type);
                    return compareValues(sensorValue, operation, requiredValue);
                });
    }

    private Integer getSensorValueByType(SensorStateAvro state, String type) {
        Object data = state.getData();

        return switch (type) {
            case "TEMPERATURE" -> {
                // Сначала проверяем TemperatureSensorAvro
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro temp) {
                    yield temp.getTemperatureC();
                }
                // Затем ClimateSensorAvro (он тоже содержит температуру)
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                }
                yield null;
            }
            case "LUMINOSITY" -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro light) {
                    yield light.getLuminosity();
                }
                yield null;
            }
            case "CO2LEVEL" -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro climate) {
                    yield climate.getCo2Level();
                }
                yield null;
            }
            case "HUMIDITY" -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro climate) {
                    yield climate.getHumidity();
                }
                yield null;
            }
            case "MOTION" -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro motion) {
                    yield motion.getMotion() ? 1 : 0;
                }
                yield null;
            }
            case "SWITCH" -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro sw) {
                    yield sw.getState() ? 1 : 0;
                }
                yield null;
            }
            default -> null;
        };
    }

    private boolean compareValues(Integer sensorValue, String operation, Integer requiredValue) {
        if (sensorValue == null || requiredValue == null) {
            return false;
        }

        return switch (operation) {
            case "EQUALS" -> sensorValue.equals(requiredValue);
            case "GREATER_THAN" -> sensorValue > requiredValue;
            case "LOWER_THAN" -> sensorValue < requiredValue;
            default -> false;
        };
    }

    private void executeScenarioActions(Scenario scenario, String hubId,
                                        Map<String, SensorStateAvro> sensorsState) {
        if (scenario.getActions() == null || scenario.getActions().isEmpty()) {
            return;
        }

        for (Action action : scenario.getActions()) {
            executeAction(action, hubId, scenario.getName());
        }
    }

    private void executeAction(Action action, String hubId, String scenarioName) {
        try {
            log.info("Отправляю действие: hubId={}, scenario={}, sensorId={}, type={}, value={}",
                    hubId, scenarioName, action.getSensorId(), action.getType(), action.getValue());

            DeviceActionProto deviceAction = DeviceActionProto.newBuilder()
                    .setSensorId(action.getSensorId() != null ? action.getSensorId() : "")
                    .setType(ActionTypeProto.valueOf(action.getType()))
                    .setValue(action.getValue() != null ? action.getValue() : 0)
                    .build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(deviceAction)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .build())
                    .build();

            hubRouterClient.handleDeviceAction(request);
            log.info("✓ Действие отправлено успешно");
        } catch (Exception e) {
            log.error("✗ Ошибка: {}", e.getMessage());
        }
    }
}