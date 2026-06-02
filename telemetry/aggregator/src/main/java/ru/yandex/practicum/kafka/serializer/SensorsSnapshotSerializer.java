package ru.yandex.practicum.kafka.serializer;

import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;

public class SensorsSnapshotSerializer implements Serializer<SensorsSnapshotAvro> {

    @Override
    public byte[] serialize(String topic, SensorsSnapshotAvro data) {
        if (data == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            SpecificDatumWriter<SensorsSnapshotAvro> writer = new SpecificDatumWriter<>(data.getSchema());
            writer.write(data, EncoderFactory.get().binaryEncoder(out, null));
            out.close();
            byte[] result = out.toByteArray();

            // ПОДРОБНОЕ ЛОГИРОВАНИЕ
            System.err.println("=== СЕРИАЛИЗАЦИЯ ===");
            System.err.println("Schema: " + data.getSchema().getFullName());
            System.err.println("Size: " + result.length);
            System.err.println("HEX: " + HexFormat.of().formatHex(result));

            return result;
        } catch (IOException e) {
            throw new SerializationException("Ошибка сериализации SensorsSnapshotAvro", e);
        }
    }
}