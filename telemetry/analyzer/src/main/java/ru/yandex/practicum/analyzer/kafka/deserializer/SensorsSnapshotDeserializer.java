package ru.yandex.practicum.analyzer.kafka.deserializer;

import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.io.IOException;
import java.util.Map;

public class SensorsSnapshotDeserializer implements Deserializer<SensorsSnapshotAvro> {

    private final DecoderFactory decoderFactory = DecoderFactory.get();
    private final Schema schema;
    private final DatumReader<SensorsSnapshotAvro> reader;

    public SensorsSnapshotDeserializer() {
        this.schema = SensorsSnapshotAvro.getClassSchema();
        this.reader = new SpecificDatumReader<>(schema);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public SensorsSnapshotAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            Decoder decoder = decoderFactory.binaryDecoder(data, null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new SerializationException("Ошибка десериализации SensorsSnapshotAvro из топика: " + topic, e);
        }
    }

    @Override
    public void close() {
    }
}