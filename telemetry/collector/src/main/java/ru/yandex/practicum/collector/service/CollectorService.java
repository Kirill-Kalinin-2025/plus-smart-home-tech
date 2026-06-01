package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.dto.*;
import ru.yandex.practicum.collector.enums.DeviceType;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorService {

    private final KafkaProducer<String, byte[]> kafkaProducer;

    @Value("${kafka.topic.sensors}")
    private String sensorsTopic;

    @Value("${kafka.topic.hubs}")
    private String hubsTopic;

    public void processSensorEvent(SensorEvent event) {
        byte[] avroBytes = mapToSensorAvro(event);
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(sensorsTopic, event.getHubId(), avroBytes);
        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Error sending sensor event: {}", exception.getMessage());
            } else {
                log.debug("Sensor event sent to topic {}: partition {}, offset {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    public void processHubEvent(HubEvent event) {
        byte[] avroBytes = mapToHubAvro(event);
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(hubsTopic, event.getHubId(), avroBytes);
        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Error sending hub event: {}", exception.getMessage());
            } else {
                log.debug("Hub event sent to topic {}: partition {}, offset {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    private byte[] mapToSensorAvro(SensorEvent event) {
        try {
            SensorEventAvro avro = SensorEventAvro.newBuilder()
                    .setId(event.getId())
                    .setHubId(event.getHubId())
                    .setTimestamp(event.getTimestamp())
                    .setPayload(createSensorPayload(event))
                    .build();

            return serializeAvro(avro);
        } catch (IOException e) {
            log.error("Error serializing sensor event: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Object createSensorPayload(SensorEvent event) {
        return switch (event.getType()) {
            case CLIMATE_SENSOR_EVENT -> {
                ClimateSensorEvent e = (ClimateSensorEvent) event;
                yield ClimateSensorAvro.newBuilder()
                        .setTemperatureC(e.getTemperatureC())
                        .setHumidity(e.getHumidity())
                        .setCo2Level(e.getCo2Level())
                        .build();
            }
            case LIGHT_SENSOR_EVENT -> {
                LightSensorEvent e = (LightSensorEvent) event;
                yield LightSensorAvro.newBuilder()
                        .setLinkQuality(e.getLinkQuality())
                        .setLuminosity(e.getLuminosity())
                        .build();
            }
            case MOTION_SENSOR_EVENT -> {
                MotionSensorEvent e = (MotionSensorEvent) event;
                yield MotionSensorAvro.newBuilder()
                        .setLinkQuality(e.getLinkQuality())
                        .setMotion(e.isMotion())
                        .setVoltage(e.getVoltage())
                        .build();
            }
            case SWITCH_SENSOR_EVENT -> {
                SwitchSensorEvent e = (SwitchSensorEvent) event;
                yield SwitchSensorAvro.newBuilder()
                        .setState(e.isState())
                        .build();
            }
            case TEMPERATURE_SENSOR_EVENT -> {
                TemperatureSensorEvent e = (TemperatureSensorEvent) event;
                yield TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(e.getTemperatureC())
                        .setTemperatureF(e.getTemperatureF())
                        .build();
            }
        };
    }

    private byte[] mapToHubAvro(HubEvent event) {
        try {
            HubEventAvro avro = HubEventAvro.newBuilder()
                    .setHubId(event.getHubId())
                    .setTimestamp(event.getTimestamp())
                    .setPayload(createHubPayload(event))
                    .build();

            return serializeAvro(avro);
        } catch (IOException e) {
            log.error("Error serializing hub event: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Object createHubPayload(HubEvent event) {
        return switch (event.getType()) {
            case DEVICE_ADDED -> {
                DeviceAddedEvent e = (DeviceAddedEvent) event;
                yield DeviceAddedEventAvro.newBuilder()
                        .setId(e.getId())
                        .setType(DeviceTypeAvro.valueOf(e.getDeviceType().name()))
                        .build();
            }
            case DEVICE_REMOVED -> {
                DeviceRemovedEvent e = (DeviceRemovedEvent) event;
                yield DeviceRemovedEventAvro.newBuilder()
                        .setId(e.getId())
                        .build();
            }
            case SCENARIO_ADDED -> {
                ScenarioAddedEvent e = (ScenarioAddedEvent) event;
                yield ScenarioAddedEventAvro.newBuilder()
                        .setName(e.getName())
                        .setConditions(e.getConditions().stream()
                                .map(this::mapCondition)
                                .collect(Collectors.toList()))
                        .setActions(e.getActions().stream()
                                .map(this::mapAction)
                                .collect(Collectors.toList()))
                        .build();
            }
            case SCENARIO_REMOVED -> {
                ScenarioRemovedEvent e = (ScenarioRemovedEvent) event;
                yield ScenarioRemovedEventAvro.newBuilder()
                        .setName(e.getName())
                        .build();
            }
        };
    }

    private ScenarioConditionAvro mapCondition(ScenarioAddedEvent.ScenarioCondition condition) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(ConditionTypeAvro.valueOf(condition.getType()))
                .setOperation(ConditionOperationAvro.valueOf(condition.getOperation()));
        if (condition.getValue() != null) {
            builder.setValue(condition.getValue());
        }
        return builder.build();
    }

    private DeviceActionAvro mapAction(ScenarioAddedEvent.DeviceAction action) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(ActionTypeAvro.valueOf(action.getType()));
        if (action.getValue() != null) {
            builder.setValue(action.getValue());
        }
        return builder.build();
    }

    private byte[] serializeAvro(SpecificRecordBase avroRecord) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        SpecificDatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(avroRecord.getSchema());
        writer.write(avroRecord, encoder);
        encoder.flush();
        byte[] bytes = out.toByteArray();
        log.info("Avro serialized: schema={}, class={}, size={} bytes",
                avroRecord.getSchema().getName(),
                avroRecord.getClass().getSimpleName(),
                bytes.length);
        return bytes;
    }
}