package ru.yandex.practicum.kafka.serializer;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;

public class GeneralAvroSerializer implements Serializer<SpecificRecordBase> {

    @Override
    public byte[] serialize(String topic, SpecificRecordBase data) {
        if (data == null) {
            return null;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            SpecificDatumWriter<SpecificRecordBase> writer =
                    new SpecificDatumWriter<>(data.getSchema());
            writer.write(data, encoder);
            encoder.flush();
            byte[] result = out.toByteArray();

            // Логирование для отладки
            System.err.println("=== СЕРИАЛИЗАЦИЯ ===");
            System.err.println("Schema: " + data.getSchema().getFullName());
            System.err.println("Class: " + data.getClass().getSimpleName());
            System.err.println("Size: " + result.length);
            System.err.println("HEX: " + HexFormat.of().formatHex(result));

            return result;
        } catch (IOException e) {
            throw new SerializationException("Ошибка сериализации " +
                    data.getClass().getSimpleName(), e);
        }
    }
}