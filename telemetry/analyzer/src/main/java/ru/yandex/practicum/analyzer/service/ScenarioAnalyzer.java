package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.entity.*;
import ru.yandex.practicum.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioAnalyzer {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    @Transactional(readOnly = true)
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
                executeScenarioActions(scenario);
            } else {
                log.info("✗ Сценарий '{}' не активирован для хаба {}", scenario.getName(), hubId);
            }
        }
    }

    private boolean checkScenarioConditions(Scenario scenario,
                                            Map<String, SensorStateAvro> sensorsState) {
        List<ScenarioCondition> scenarioConditions =
                scenarioConditionRepository.findByScenario(scenario);

        if (scenarioConditions.isEmpty()) {
            log.warn("У сценария '{}' нет условий", scenario.getName());
            return false;
        }

        log.info("Проверяю {} условий для сценария '{}'", scenarioConditions.size(), scenario.getName());

        // Все условия должны выполняться (noneMatch + отрицание = allMatch)
        return scenarioConditions.stream()
                .noneMatch(sc -> !checkCondition(sc.getCondition(), sc.getSensor().getId(), sensorsState));
    }

    private boolean checkCondition(Condition condition, String sensorId,
                                   Map<String, SensorStateAvro> sensorStateMap) {
        SensorStateAvro sensorState = sensorStateMap.get(sensorId);
        if (sensorState == null) {
            log.debug("Датчик {} не найден в снапшоте", sensorId);
            return false;
        }

        Integer currentValue = getSensorValue(condition.getType(), sensorState);
        if (currentValue == null) {
            log.debug("Не удалось получить значение для датчика {} типа {}", sensorId, condition.getType());
            return false;
        }

        Integer targetValue = condition.getValue();

        boolean result = switch (condition.getOperation()) {
            case EQUALS -> currentValue.equals(targetValue);
            case GREATER_THAN -> currentValue > targetValue;
            case LOWER_THAN -> currentValue < targetValue;
        };

        log.debug("Датчик {} ({}): текущее={}, операция={}, целевое={} -> {}",
                sensorId, condition.getType(), currentValue, condition.getOperation(), targetValue, result);

        return result;
    }

    private Integer getSensorValue(ConditionTypeAvro conditionType, SensorStateAvro sensorState) {
        Object data = sensorState.getData();

        return switch (conditionType) {
            case MOTION -> {
                if (data instanceof MotionSensorAvro motion) {
                    yield motion.getMotion() ? 1 : 0;
                }
                yield null;
            }
            case LUMINOSITY -> {
                if (data instanceof LightSensorAvro light) {
                    yield light.getLuminosity();
                }
                yield null;
            }
            case SWITCH -> {
                if (data instanceof SwitchSensorAvro sw) {
                    yield sw.getState() ? 1 : 0;
                }
                yield null;
            }
            case TEMPERATURE -> {
                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                }
                if (data instanceof TemperatureSensorAvro temp) {
                    yield temp.getTemperatureC();
                }
                yield null;
            }
            case CO2LEVEL -> {
                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getCo2Level();
                }
                yield null;
            }
            case HUMIDITY -> {
                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getHumidity();
                }
                yield null;
            }
        };
    }

    private void executeScenarioActions(Scenario scenario) {
        List<ScenarioAction> scenarioActions =
                scenarioActionRepository.findByScenario(scenario);

        log.info("Отправляю {} действий для сценария '{}'", scenarioActions.size(), scenario.getName());

        for (ScenarioAction scenarioAction : scenarioActions) {
            sendAction(scenarioAction);
        }
    }

    private void sendAction(ScenarioAction scenarioAction) {
        try {
            Scenario scenario = scenarioAction.getScenario();
            Sensor sensor = scenarioAction.getSensor();
            Action action = scenarioAction.getAction();

            log.info("Отправляю действие: hubId={}, scenario={}, sensorId={}, type={}, value={}",
                    scenario.getHubId(), scenario.getName(), sensor.getId(), action.getType(), action.getValue());

            DeviceActionProto deviceAction = DeviceActionProto.newBuilder()
                    .setSensorId(sensor.getId())
                    .setType(ActionTypeProto.valueOf(action.getType().name()))
                    .setValue(action.getValue() != null ? action.getValue() : 0)
                    .build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(scenario.getHubId())
                    .setScenarioName(scenario.getName())
                    .setAction(deviceAction)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            hubRouterClient.handleDeviceAction(request);
            log.info("✓ Действие отправлено успешно");
        } catch (Exception e) {
            log.error("✗ Ошибка при отправке действия: {}", e.getMessage(), e);
        }
    }
}