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
import ru.yandex.practicum.grpc.telemetry.event.*;
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

    public void processSensorEvent(SensorEventProto event) {
        byte[] avroBytes = mapToSensorAvro(event);
        kafkaProducer.send(new ProducerRecord<>(sensorsTopic, event.getHubId(), avroBytes),
                (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Error sending sensor event: {}", exception.getMessage());
                    } else {
                        log.debug("Sensor event sent to topic {}: partition {}, offset {}",
                                metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });
    }

    public void processHubEvent(HubEventProto event) {
        byte[] avroBytes = mapToHubAvro(event);
        kafkaProducer.send(new ProducerRecord<>(hubsTopic, event.getHubId(), avroBytes),
                (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Error sending hub event: {}", exception.getMessage());
                    } else {
                        log.debug("Hub event sent to topic {}: partition {}, offset {}",
                                metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });
    }

    private byte[] mapToSensorAvro(SensorEventProto event) {
        try {
            SensorEventAvro avro = SensorEventAvro.newBuilder()
                    .setId(event.getId())
                    .setHubId(event.getHubId())
                    .setTimestamp(convertTimestamp(event.getTimestamp()))
                    .setPayload(createSensorPayload(event))
                    .build();
            return serializeAvro(avro);
        } catch (IOException e) {
            log.error("Error serializing sensor event: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Object createSensorPayload(SensorEventProto event) {
        return switch (event.getPayloadCase()) {
            case MOTION_SENSOR -> {
                MotionSensorProto m = event.getMotionSensor();
                yield MotionSensorAvro.newBuilder()
                        .setLinkQuality(m.getLinkQuality())
                        .setMotion(m.getMotion())
                        .setVoltage(m.getVoltage())
                        .build();
            }
            case TEMPERATURE_SENSOR -> {
                TemperatureSensorProto t = event.getTemperatureSensor();
                yield TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(t.getTemperatureC())
                        .setTemperatureF(t.getTemperatureF())
                        .build();
            }
            case LIGHT_SENSOR -> {
                LightSensorProto l = event.getLightSensor();
                yield LightSensorAvro.newBuilder()
                        .setLinkQuality(l.getLinkQuality())
                        .setLuminosity(l.getLuminosity())
                        .build();
            }
            case CLIMATE_SENSOR -> {
                ClimateSensorProto c = event.getClimateSensor();
                yield ClimateSensorAvro.newBuilder()
                        .setTemperatureC(c.getTemperatureC())
                        .setHumidity(c.getHumidity())
                        .setCo2Level(c.getCo2Level())
                        .build();
            }
            case SWITCH_SENSOR -> {
                SwitchSensorProto s = event.getSwitchSensor();
                yield SwitchSensorAvro.newBuilder()
                        .setState(s.getState())
                        .build();
            }
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException("Payload not set");
        };
    }

    private byte[] mapToHubAvro(HubEventProto event) {
        try {
            HubEventAvro avro = HubEventAvro.newBuilder()
                    .setHubId(event.getHubId())
                    .setTimestamp(convertTimestamp(event.getTimestamp()))
                    .setPayload(createHubPayload(event))
                    .build();
            return serializeAvro(avro);
        } catch (IOException e) {
            log.error("Error serializing hub event: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Object createHubPayload(HubEventProto event) {
        return switch (event.getPayloadCase()) {
            case DEVICE_ADDED -> {
                DeviceAddedEventProto d = event.getDeviceAdded();
                yield DeviceAddedEventAvro.newBuilder()
                        .setId(d.getId())
                        .setType(DeviceTypeAvro.valueOf(d.getType().name()))
                        .build();
            }
            case DEVICE_REMOVED -> {
                DeviceRemovedEventProto d = event.getDeviceRemoved();
                yield DeviceRemovedEventAvro.newBuilder()
                        .setId(d.getId())
                        .build();
            }
            case SCENARIO_ADDED -> {
                ScenarioAddedEventProto s = event.getScenarioAdded();
                yield ScenarioAddedEventAvro.newBuilder()
                        .setName(s.getName())
                        .setConditions(s.getConditionList().stream()
                                .map(this::mapCondition)
                                .collect(Collectors.toList()))
                        .setActions(s.getActionList().stream()
                                .map(this::mapAction)
                                .collect(Collectors.toList()))
                        .build();
            }
            case SCENARIO_REMOVED -> {
                ScenarioRemovedEventProto s = event.getScenarioRemoved();
                yield ScenarioRemovedEventAvro.newBuilder()
                        .setName(s.getName())
                        .build();
            }
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException("Payload not set");
        };
    }

    private ScenarioConditionAvro mapCondition(ScenarioConditionProto c) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(c.getSensorId())
                .setType(ConditionTypeAvro.valueOf(c.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()));
        switch (c.getValueCase()) {
            case BOOL_VALUE -> builder.setValue(c.getBoolValue());
            case INT_VALUE -> builder.setValue(c.getIntValue());
        }
        return builder.build();
    }

    private DeviceActionAvro mapAction(DeviceActionProto a) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                .setSensorId(a.getSensorId())
                .setType(ActionTypeAvro.valueOf(a.getType().name()));
        if (a.hasValue()) {
            builder.setValue(a.getValue());
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
                avroRecord.getSchema().getName(), avroRecord.getClass().getSimpleName(), bytes.length);
        return bytes;
    }

    private java.time.Instant convertTimestamp(com.google.protobuf.Timestamp timestamp) {
        return java.time.Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}