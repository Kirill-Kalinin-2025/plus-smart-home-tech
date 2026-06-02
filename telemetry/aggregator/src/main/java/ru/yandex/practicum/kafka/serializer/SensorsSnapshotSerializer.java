package ru.yandex.practicum.kafka.serializer;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.io.IOException;

public class SensorsSnapshotSerializer implements Serializer<SensorsSnapshotAvro> {

    @Override
    public byte[] serialize(String topic, SensorsSnapshotAvro data) {
        if (data == null) {
            return null;
        }
        try {
            return data.toByteBuffer().array();
        } catch (IOException e) {
            throw new SerializationException("Ошибка сериализации SensorsSnapshotAvro", e);
        }
    }
}