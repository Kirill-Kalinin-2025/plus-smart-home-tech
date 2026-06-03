package ru.yandex.practicum.kafka.serializer;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SensorsSnapshotSerializer implements Serializer<SensorsSnapshotAvro> {

    @Override
    public byte[] serialize(String topic, SensorsSnapshotAvro data) {
        if (data == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            SpecificDatumWriter<SensorsSnapshotAvro> writer = new SpecificDatumWriter<>(data.getSchema());
            writer.write(data, encoder);
            encoder.flush();  // ← ДОБАВИТЬ!
            return out.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Ошибка сериализации SensorsSnapshotAvro", e);
        }
    }
}