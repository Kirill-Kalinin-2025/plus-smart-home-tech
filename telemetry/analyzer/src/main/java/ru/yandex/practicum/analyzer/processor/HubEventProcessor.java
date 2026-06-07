package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.entity.*;
import ru.yandex.practicum.analyzer.repository.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final Consumer<String, SpecificRecordBase> hubConsumer;
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ActionRepository actionRepository;
    private final ConditionRepository conditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;

    @Value("${analyzer.kafka.topics.hubs}")
    private String hubsTopic;

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(hubConsumer::wakeup));

        try {
            hubConsumer.subscribe(List.of(hubsTopic));
            log.info("HubEventProcessor подписан на топик: {}", hubsTopic);

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, SpecificRecordBase> records =
                        hubConsumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    if (record.value() instanceof HubEventAvro hubEvent) {
                        processHubEvent(hubEvent);
                    }
                }
                hubConsumer.commitSync();
            }
        } catch (WakeupException e) {
            log.info("HubEventProcessor получил сигнал завершения");
        } catch (Exception e) {
            log.error("Ошибка в HubEventProcessor", e);
        } finally {
            try {
                hubConsumer.commitSync();
            } finally {
                hubConsumer.close();
                log.info("HubEventProcessor закрыт");
            }
        }
    }

    private void processHubEvent(HubEventAvro event) {
        log.info("Получено событие хаба: hubId={}", event.getHubId());

        Object payload = event.getPayload();

        if (payload instanceof DeviceAddedEventAvro deviceAdded) {
            handleDeviceAdded(event.getHubId(), deviceAdded);
        } else if (payload instanceof DeviceRemovedEventAvro deviceRemoved) {
            handleDeviceRemoved(event.getHubId(), deviceRemoved);
        } else if (payload instanceof ScenarioAddedEventAvro scenarioAdded) {
            handleScenarioAdded(event.getHubId(), scenarioAdded);
        } else if (payload instanceof ScenarioRemovedEventAvro scenarioRemoved) {
            handleScenarioRemoved(event.getHubId(), scenarioRemoved);
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro deviceAdded) {
        String sensorId = deviceAdded.getId();
        if (!sensorRepository.existsById(sensorId)) {
            Sensor sensor = Sensor.builder()
                    .id(sensorId)
                    .hubId(hubId)
                    .build();
            sensorRepository.save(sensor);
            log.info("Добавлен новый датчик: sensorId={}, hubId={}", sensorId, hubId);
        } else {
            log.debug("Датчик уже существует: sensorId={}", sensorId);
        }
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro deviceRemoved) {
        String sensorId = deviceRemoved.getId();
        sensorRepository.deleteByIdAndHubId(sensorId, hubId);
        log.info("Удален датчик: sensorId={}, hubId={}", sensorId, hubId);
    }

    @Transactional
    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro scenarioAdded) {
        String scenarioName = scenarioAdded.getName();

        // Находим или создаём сценарий
        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, scenarioName)
                .orElseGet(() -> scenarioRepository.save(
                        Scenario.builder()
                                .hubId(hubId)
                                .name(scenarioName)
                                .build()));

        // Удаляем старые связи
        scenarioActionRepository.deleteByScenario(scenario);
        scenarioConditionRepository.deleteByScenario(scenario);

        // Обрабатываем условия
        scenarioAdded.getConditions().forEach(cDto -> {
            Sensor sensor = sensorRepository.findById(cDto.getSensorId())
                    .orElseGet(() -> sensorRepository.save(
                            Sensor.builder()
                                    .id(cDto.getSensorId())
                                    .hubId(hubId)
                                    .build()));

            Condition condition = conditionRepository.save(
                    Condition.builder()
                            .type(cDto.getType())
                            .operation(cDto.getOperation())
                            .value(getConditionValue(cDto))
                            .build());

            scenarioConditionRepository.save(
                    ScenarioCondition.builder()
                            .id(new ScenarioConditionId(scenario.getId(), sensor.getId(), condition.getId()))
                            .scenario(scenario)
                            .sensor(sensor)
                            .condition(condition)
                            .build());
        });

        // Обрабатываем действия
        scenarioAdded.getActions().forEach(aDto -> {
            Sensor sensor = sensorRepository.findById(aDto.getSensorId())
                    .orElseGet(() -> sensorRepository.save(
                            Sensor.builder()
                                    .id(aDto.getSensorId())
                                    .hubId(hubId)
                                    .build()));

            Action action = actionRepository.save(
                    Action.builder()
                            .type(aDto.getType())
                            .value(aDto.getValue() != null ? aDto.getValue() : null)
                            .build());

            scenarioActionRepository.save(
                    ScenarioAction.builder()
                            .id(new ScenarioActionId(scenario.getId(), sensor.getId(), action.getId()))
                            .scenario(scenario)
                            .sensor(sensor)
                            .action(action)
                            .build());
        });

        log.info("Добавлен/обновлён сценарий: hubId={}, name={}", hubId, scenarioName);
    }

    @Transactional
    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro scenarioRemoved) {
        String scenarioName = scenarioRemoved.getName();
        scenarioRepository.findByHubIdAndName(hubId, scenarioName)
                .ifPresent(scenario -> {
                    scenarioActionRepository.deleteByScenario(scenario);
                    scenarioConditionRepository.deleteByScenario(scenario);
                    scenarioRepository.delete(scenario);
                    log.info("Удален сценарий: hubId={}, name={}", hubId, scenarioName);
                });
    }

    private Integer getConditionValue(ScenarioConditionAvro condition) {
        Object value = condition.getValue();
        if (value instanceof Integer intValue) {
            return intValue;
        } else if (value instanceof Boolean boolValue) {
            return boolValue ? 1 : 0;
        }
        return null;
    }
}