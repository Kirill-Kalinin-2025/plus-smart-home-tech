package ru.yandex.practicum.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final Consumer<String, SensorEventAvro> consumer;
    private final Producer<String, SensorsSnapshotAvro> producer;
    private final String inputTopic = "telemetry.sensors.v1";
    private final String outputTopic = "telemetry.snapshots.v1";

    // Хранилище снапшотов (hubId -> SensorsSnapshotAvro)
    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public void start() {
        try {
            consumer.subscribe(java.util.List.of(inputTopic));
            log.info("Подписались на топик: {}", inputTopic);

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();
                    log.debug("Получено событие от датчика: id={}, hubId={}", event.getId(), event.getHubId());

                    Optional<SensorsSnapshotAvro> updatedSnapshot = updateState(event);

                    if (updatedSnapshot.isPresent()) {
                        SensorsSnapshotAvro snapshot = updatedSnapshot.get();
                        ProducerRecord<String, SensorsSnapshotAvro> producerRecord = new ProducerRecord<>(
                                outputTopic, snapshot.getHubId(), snapshot
                        );
                        producer.send(producerRecord);
                        log.info("Отправлен обновлённый снапшот для хаба: {}", snapshot.getHubId());
                    }
                }

                consumer.commitSync();
            }
        } catch (WakeupException e) {
            log.info("Получен сигнал завершения работы");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                producer.flush();
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер и продюсер");
                consumer.close();
                producer.close();
            }
        }
    }

    private Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();
        Instant eventTimestamp = event.getTimestamp();

        // Получаем существующий снапшот или создаём новый
        SensorsSnapshotAvro snapshot = snapshots.get(hubId);
        if (snapshot == null) {
            snapshot = SensorsSnapshotAvro.newBuilder()
                    .setHubId(hubId)
                    .setTimestamp(eventTimestamp)  // Instant
                    .setSensorsState(new HashMap<>())
                    .build();
            snapshots.put(hubId, snapshot);
            log.info("Создан новый снапшот для хаба: {}", hubId);
            return Optional.of(snapshot);
        }

        // Получаем существующее состояние датчика
        SensorStateAvro oldState = snapshot.getSensorsState().get(sensorId);

        // Проверяем, нужно ли обновлять состояние
        if (oldState != null && oldState.getTimestamp().isAfter(eventTimestamp)) {
            // Полученное событие устарело
            log.debug("Событие от датчика {} устарело. timestamp события: {}, timestamp в снапшоте: {}",
                    sensorId, eventTimestamp, oldState.getTimestamp());
            return Optional.empty();
        }

        // Проверяем, изменились ли данные
        if (oldState != null && oldState.getData().equals(event.getPayload())) {
            // Данные не изменились
            log.debug("Данные датчика {} не изменились", sensorId);
            return Optional.empty();
        }

        // Создаём новое состояние датчика
        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(eventTimestamp)  // Instant
                .setData(event.getPayload())
                .build();

        // Обновляем снапшот
        snapshot.getSensorsState().put(sensorId, newState);
        snapshot.setTimestamp(eventTimestamp);  // Instant

        log.info("Обновлён датчик {} в снапшоте хаба {}", sensorId, hubId);

        return Optional.of(snapshot);
    }
}